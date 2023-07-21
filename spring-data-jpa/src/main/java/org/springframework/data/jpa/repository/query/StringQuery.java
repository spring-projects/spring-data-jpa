/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.query;

import static java.util.regex.Pattern.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.InParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.query.SpelQueryContext.SpelExtractor;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulation of a JPA query String. Offers access to parameters as bindings. The internal query String is cleaned
 * from decorated parameters like {@literal %:lastname%} and the matching bindings take care of applying the decorations
 * in the {@link ParameterBinding#prepare(Object)} method. Note that this class also handles replacing SpEL expressions
 * with synthetic bind parameters
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Oliver Wehrens
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Diego Krupitza
 * @author Greg Turnquist
 * @author Yuriy Tsarkov
 */
class StringQuery implements DeclaredQuery {

	private final String query;
	private final List<ParameterBinding> bindings;
	private final @Nullable String alias;
	private final boolean hasConstructorExpression;
	private final boolean containsPageableInSpel;
	private final boolean usesJdbcStyleParameters;
	private final boolean isNative;
	private final QueryEnhancer queryEnhancer;

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 *
	 * @param query must not be {@literal null} or empty.
	 */
	@SuppressWarnings("deprecation")
	StringQuery(String query, boolean isNative) {

		Assert.hasText(query, "Query must not be null or empty");

		this.isNative = isNative;
		this.bindings = new ArrayList<>();
		this.containsPageableInSpel = query.contains("#pageable");

		Metadata queryMeta = new Metadata();
		this.query = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query,
				this.bindings, queryMeta);

		this.usesJdbcStyleParameters = queryMeta.usesJdbcStyleParameters;

		this.queryEnhancer = QueryEnhancerFactory.forQuery(this);
		this.alias = this.queryEnhancer.detectAlias();
		this.hasConstructorExpression = this.queryEnhancer.hasConstructorExpression();
	}

	/**
	 * Returns whether we have found some like bindings.
	 */
	boolean hasParameterBindings() {
		return !bindings.isEmpty();
	}

	String getProjection() {
		return this.queryEnhancer.getProjection();
	}

	@Override
	public List<ParameterBinding> getParameterBindings() {
		return bindings;
	}

	@Override
	public DeclaredQuery deriveCountQuery(@Nullable String countQuery, @Nullable String countQueryProjection) {

		return DeclaredQuery.of( //
				countQuery != null ? countQuery : this.queryEnhancer.createCountQueryFor(countQueryProjection), //
				this.isNative);
	}

	@Override
	public boolean usesJdbcStyleParameters() {
		return usesJdbcStyleParameters;
	}

	@Override
	public String getQueryString() {
		return query;
	}

	@Override
	@Nullable
	public String getAlias() {
		return alias;
	}

	@Override
	public boolean hasConstructorExpression() {
		return hasConstructorExpression;
	}

	@Override
	public boolean isDefaultProjection() {
		return getProjection().equalsIgnoreCase(alias);
	}

	@Override
	public boolean hasNamedParameter() {
		return bindings.stream().anyMatch(b -> b.getIdentifier().hasName());
	}

	@Override
	public boolean usesPaging() {
		return containsPageableInSpel;
	}

	@Override
	public boolean isNativeQuery() {
		return isNative;
	}

	/**
	 * A parser that extracts the parameter bindings from a given query string.
	 *
	 * @author Thomas Darimont
	 */
	enum ParameterBindingParser {

		INSTANCE;

		private static final String EXPRESSION_PARAMETER_PREFIX = "__$synthetic$__";
		public static final String POSITIONAL_OR_INDEXED_PARAMETER = "\\?(\\d*+(?![#\\w]))";
		// .....................................................................^ not followed by a hash or a letter.
		// .................................................................^ zero or more digits.
		// .............................................................^ start with a question mark.
		private static final Pattern PARAMETER_BINDING_BY_INDEX = Pattern.compile(POSITIONAL_OR_INDEXED_PARAMETER);
		private static final Pattern PARAMETER_BINDING_PATTERN;
		private static final Pattern JDBC_STYLE_PARAM = Pattern.compile(" \\?(?!\\d)"); // <space>?[no digit]
		private static final Pattern NUMBERED_STYLE_PARAM = Pattern.compile(" \\?(?=\\d)"); // <space>?[digit]
		private static final Pattern NAMED_STYLE_PARAM = Pattern.compile(" :\\w+"); // <space>:[text]

		private static final String MESSAGE = "Already found parameter binding with same index / parameter name but differing binding type; "
				+ "Already have: %s, found %s; If you bind a parameter multiple times make sure they use the same binding";
		private static final int INDEXED_PARAMETER_GROUP = 4;
		private static final int NAMED_PARAMETER_GROUP = 6;
		private static final int COMPARISION_TYPE_GROUP = 1;

		static {

			List<String> keywords = new ArrayList<>();

			for (ParameterBindingType type : ParameterBindingType.values()) {
				if (type.getKeyword() != null) {
					keywords.add(type.getKeyword());
				}
			}

			StringBuilder builder = new StringBuilder();
			builder.append("(");
			builder.append(StringUtils.collectionToDelimitedString(keywords, "|")); // keywords
			builder.append(")?");
			builder.append("(?: )?"); // some whitespace
			builder.append("\\(?"); // optional braces around parameters
			builder.append("(");
			builder.append("%?(" + POSITIONAL_OR_INDEXED_PARAMETER + ")%?"); // position parameter and parameter index
			builder.append("|"); // or

			// named parameter and the parameter name
			builder.append("%?(" + QueryUtils.COLON_NO_DOUBLE_COLON + QueryUtils.IDENTIFIER_GROUP + ")%?");

			builder.append(")");
			builder.append("\\)?"); // optional braces around parameters

			PARAMETER_BINDING_PATTERN = Pattern.compile(builder.toString(), CASE_INSENSITIVE);
		}

		/**
		 * Parses {@link ParameterBinding} instances from the given query and adds them to the registered bindings. Returns
		 * the cleaned up query.
		 */
		private String parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(String query,
				List<ParameterBinding> bindings, Metadata queryMeta) {

			int greatestParameterIndex = tryFindGreatestParameterIndexIn(query);
			boolean parametersShouldBeAccessedByIndex = greatestParameterIndex != -1;

			/*
			 * Prefer indexed access over named parameters if only SpEL Expression parameters are present.
			 */
			if (!parametersShouldBeAccessedByIndex && query.contains("?#{")) {
				parametersShouldBeAccessedByIndex = true;
				greatestParameterIndex = 0;
			}

			SpelExtractor spelExtractor = createSpelExtractor(query, parametersShouldBeAccessedByIndex,
					greatestParameterIndex);

			String resultingQuery = spelExtractor.getQueryString();
			Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(resultingQuery);

			int expressionParameterIndex = parametersShouldBeAccessedByIndex ? greatestParameterIndex : 0;

			LikeParameterBindings likeParameterBindings = new LikeParameterBindings();

			boolean usesJpaStyleParameters = false;
			while (matcher.find()) {

				if (spelExtractor.isQuoted(matcher.start())) {
					continue;
				}

				String parameterIndexString = matcher.group(INDEXED_PARAMETER_GROUP);
				String parameterName = parameterIndexString != null ? null : matcher.group(NAMED_PARAMETER_GROUP);
				Integer parameterIndex = getParameterIndex(parameterIndexString);

				String typeSource = matcher.group(COMPARISION_TYPE_GROUP);
				Assert.isTrue(parameterIndexString != null || parameterName != null,
						() -> String.format("We need either a name or an index; Offending query string: %s", query));
				String expression = spelExtractor.getParameter(parameterName == null ? parameterIndexString : parameterName);
				String replacement = null;

				queryMeta.usesJdbcStyleParameters = JDBC_STYLE_PARAM.matcher(resultingQuery).find();
				usesJpaStyleParameters = NUMBERED_STYLE_PARAM.matcher(resultingQuery).find()
						|| NAMED_STYLE_PARAM.matcher(resultingQuery).find();

				expressionParameterIndex++;
				if ("".equals(parameterIndexString)) {
					parameterIndex = expressionParameterIndex;
				}

				if (usesJpaStyleParameters && queryMeta.usesJdbcStyleParameters) {
					throw new IllegalArgumentException("Mixing of ? parameters and other forms like ?1 is not supported");
				}

				BindingIdentifier identifier;
				if (parameterIndex != null) {
					identifier = BindingIdentifier.of(parameterIndex);
				} else {
					identifier = BindingIdentifier.of(parameterName);
				}
				ParameterOrigin origin = ObjectUtils.isEmpty(expression)
						? ParameterOrigin.ofParameter(parameterName, parameterIndex)
						: ParameterOrigin.ofExpression(expression);

				switch (ParameterBindingType.of(typeSource)) {

					case LIKE:

						Type likeType = ParameterBinding.LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
						replacement = matcher.group(3);

						if (parameterIndex != null) {
							checkAndRegister(new LikeParameterBinding(identifier, origin, likeType), bindings);
						} else {

							LikeParameterBinding binding = likeParameterBindings.getOrCreate(parameterName, likeType, origin);
							checkAndRegister(binding, bindings);

							replacement = ":" + binding.getRequiredName();
						}

						break;

					case IN:

						checkAndRegister(new InParameterBinding(identifier, origin), bindings);

						break;

					case AS_IS: // fall-through we don't need a special parameter binding for the given parameter.
					default:

						bindings.add(new ParameterBinding(identifier, origin));
				}

				if (replacement != null) {
					resultingQuery = replaceFirst(resultingQuery, matcher.group(2), replacement);
				}

			}

			return resultingQuery;
		}

		private static SpelExtractor createSpelExtractor(String queryWithSpel, boolean parametersShouldBeAccessedByIndex,
				int greatestParameterIndex) {

			/*
			 * If parameters need to be bound by index, we bind the synthetic expression parameters starting from position of the greatest discovered index parameter in order to
			 * not mix-up with the actual parameter indices.
			 */
			int expressionParameterIndex = parametersShouldBeAccessedByIndex ? greatestParameterIndex : 0;

			BiFunction<Integer, String, String> indexToParameterName = parametersShouldBeAccessedByIndex
					? (index, expression) -> String.valueOf(index + expressionParameterIndex + 1)
					: (index, expression) -> EXPRESSION_PARAMETER_PREFIX + (index + 1);

			String fixedPrefix = parametersShouldBeAccessedByIndex ? "?" : ":";

			BiFunction<String, String, String> parameterNameToReplacement = (prefix, name) -> fixedPrefix + name;

			return SpelQueryContext.of(indexToParameterName, parameterNameToReplacement).parse(queryWithSpel);
		}

		private static String replaceFirst(String text, String substring, String replacement) {

			int index = text.indexOf(substring);
			if (index < 0) {
				return text;
			}

			return text.substring(0, index) + replacement + text.substring(index + substring.length());
		}

		@Nullable
		private static Integer getParameterIndex(@Nullable String parameterIndexString) {

			if (parameterIndexString == null || parameterIndexString.isEmpty()) {
				return null;
			}
			return Integer.valueOf(parameterIndexString);
		}

		private static int tryFindGreatestParameterIndexIn(String query) {

			Matcher parameterIndexMatcher = PARAMETER_BINDING_BY_INDEX.matcher(query);

			int greatestParameterIndex = -1;
			while (parameterIndexMatcher.find()) {

				String parameterIndexString = parameterIndexMatcher.group(1);
				Integer parameterIndex = getParameterIndex(parameterIndexString);
				if (parameterIndex != null) {
					greatestParameterIndex = Math.max(greatestParameterIndex, parameterIndex);
				}
			}

			return greatestParameterIndex;
		}

		private static void checkAndRegister(ParameterBinding binding, List<ParameterBinding> bindings) {

			bindings.stream() //
					.filter(it -> it.bindsTo(binding)) //
					.forEach(it -> Assert.isTrue(it.equals(binding), String.format(MESSAGE, it, binding)));

			if (!bindings.contains(binding)) {
				bindings.add(binding);
			}
		}

		/**
		 * An enum for the different types of bindings.
		 *
		 * @author Thomas Darimont
		 * @author Oliver Gierke
		 */
		private enum ParameterBindingType {

			// Trailing whitespace is intentional to reflect that the keywords must be used with at least one whitespace
			// character, while = does not.
			LIKE("like "), IN("in "), AS_IS(null);

			private final @Nullable String keyword;

			ParameterBindingType(@Nullable String keyword) {
				this.keyword = keyword;
			}

			/**
			 * Returns the keyword that will trigger the binding type or {@literal null} if the type is not triggered by a
			 * keyword.
			 *
			 * @return the keyword
			 */
			@Nullable
			public String getKeyword() {
				return keyword;
			}

			/**
			 * Return the appropriate {@link ParameterBindingType} for the given {@link String}. Returns {@literal #AS_IS} in
			 * case no other {@link ParameterBindingType} could be found.
			 */
			static ParameterBindingType of(String typeSource) {

				if (!StringUtils.hasText(typeSource)) {
					return AS_IS;
				}

				for (ParameterBindingType type : values()) {
					if (type.name().equalsIgnoreCase(typeSource.trim())) {
						return type;
					}
				}

				throw new IllegalArgumentException(String.format("Unsupported parameter binding type %s", typeSource));
			}
		}
	}

	private static class Metadata {
		private boolean usesJdbcStyleParameters = false;
	}

	/**
	 * Utility to create unique parameter bindings for LIKE that can be evaluated by
	 * {@code LikeRewritingQueryParameterSetterFactory}.
	 *
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	static class LikeParameterBindings {

		private final MultiValueMap<String, LikeParameterBinding> likeBindings = new LinkedMultiValueMap<>();

		/**
		 * Get an existing or create a new {@link LikeParameterBinding} if a previously bound {@code LIKE} expression cannot
		 * be reused.
		 *
		 * @param parameterName the parameter name as declared in the actual JPQL query.
		 * @param likeType type of the LIKE expression.
		 * @param origin origin of the parameter.
		 * @return the Like binding. Can return an already existing binding.
		 */
		LikeParameterBinding getOrCreate(String parameterName, Type likeType, ParameterOrigin origin) {

			List<LikeParameterBinding> likeParameterBindings = likeBindings.computeIfAbsent(parameterName,
					s -> new ArrayList<>());
			LikeParameterBinding reuse = null;

			// unique parameters only required for literals as expressions create unique parameter names
			if (origin.isMethodArgument()) {
				for (LikeParameterBinding likeParameterBinding : likeParameterBindings) {

					if (likeParameterBinding.getType() == likeType) {
						reuse = likeParameterBinding;
						break;
					}
				}
			}

			if (reuse != null) {
				return reuse;
			}

			if (!likeParameterBindings.isEmpty()) {
				parameterName = parameterName + "_" + likeParameterBindings.size();
			}

			LikeParameterBinding binding = new LikeParameterBinding(BindingIdentifier.of(parameterName), origin, likeType);
			likeParameterBindings.add(binding);

			return binding;
		}
	}
}
