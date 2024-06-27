/*
 * Copyright 2013-2025 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.expression.ValueExpression;

import org.jspecify.annotations.Nullable;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.InParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.MethodInvocationArgument;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.data.repository.query.ValueExpressionQueryRewriter;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulation of a JPA query String. Offers access to parameters as bindings. The internal query String is cleaned
 * from decorated parameters like {@literal %:lastname%} and the matching bindings take care of applying the decorations
 * in the {@link ParameterBinding#prepare(Object)} method. Note that this class also handles replacing SpEL expressions
 * with synthetic bind parameters.
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
class StringQuery implements EntityQuery {

	private final String query;
	private final List<ParameterBinding> bindings;
	private final boolean containsPageableInSpel;
	private final boolean usesJdbcStyleParameters;
	private final boolean isNative;
	private final QueryEnhancerFactory queryEnhancerFactory;
	private final QueryEnhancer queryEnhancer;
	private final boolean hasNamedParameters;

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 *
	 * @param query must not be {@literal null} or empty.
	 */
	public StringQuery(String query, boolean isNative) {
		this(query, isNative, QueryEnhancerSelector.DEFAULT_SELECTOR, it -> {});
	}

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 *
	 * @param query must not be {@literal null} or empty.
	 */
	StringQuery(String query, boolean isNative, QueryEnhancerFactory factory,Consumer<List<ParameterBinding>> parameterPostProcessor) {

		Assert.hasText(query, "Query must not be null or empty");

		this.isNative = isNative;
		this.bindings = new ArrayList<>();
		this.containsPageableInSpel = query.contains("#pageable");
		this.queryEnhancerFactory = factory;

		Metadata queryMeta = new Metadata();
		this.query = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query,
				this.bindings, queryMeta);

		this.usesJdbcStyleParameters = queryMeta.usesJdbcStyleParameters;
		this.queryEnhancer = factory.create(this);

		parameterPostProcessor.accept(this.bindings);

		boolean hasNamedParameters = false;
		for (ParameterBinding parameterBinding : getParameterBindings()) {
			if (parameterBinding.getIdentifier().hasName() && parameterBinding.getOrigin().isMethodArgument()) {
				hasNamedParameters = true;
				break;
			}
		}

		this.hasNamedParameters = hasNamedParameters;
	}

	/**
	 * Creates a new {@link StringQuery} from the given JPQL query.
	 *
	 * @param query must not be {@literal null} or empty.
	 */
	StringQuery(String query, boolean isNative, QueryEnhancerSelector selector, Consumer<List<ParameterBinding>> parameterPostProcessor) {

		Assert.hasText(query, "Query must not be null or empty");

		this.isNative = isNative;
		this.bindings = new ArrayList<>();
		this.containsPageableInSpel = query.contains("#pageable");

		Metadata queryMeta = new Metadata();
		this.query = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(query,
				this.bindings, queryMeta);

		this.usesJdbcStyleParameters = queryMeta.usesJdbcStyleParameters;
		this.queryEnhancerFactory = selector.select(this);
		this.queryEnhancer = queryEnhancerFactory.create(this);

		parameterPostProcessor.accept(this.bindings);

		boolean hasNamedParameters = false;
		for (ParameterBinding parameterBinding : getParameterBindings()) {
			if (parameterBinding.getIdentifier().hasName() && parameterBinding.getOrigin().isMethodArgument()) {
				hasNamedParameters = true;
				break;
			}
		}

		this.hasNamedParameters = hasNamedParameters;
	}

	QueryEnhancer getQueryEnhancer() {
		return queryEnhancer;
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
	public IntrospectedQuery deriveCountQuery(@Nullable String countQueryProjection) {

		// need to copy expression bindings from the declared to the derived query as JPQL query derivation only sees
		// JPA parameter markers and not the original expressions anymore.

		return new StringQuery(this.queryEnhancer.createCountQueryFor(countQueryProjection), //
				this.isNative, queryEnhancerFactory, derivedBindings -> {

					// need to copy expression bindings from the declared to the derived query as JPQL query derivation only sees
					// JPA
					// parameter markers and not the original expressions anymore.
					if (this.hasParameterBindings() && !this.getParameterBindings().equals(derivedBindings)) {

						for (ParameterBinding binding : bindings) {

							Predicate<ParameterBinding> identifier = binding::bindsTo;
				Predicate<ParameterBinding> notCompatible = Predicate.not(binding::isCompatibleWith);

				// replace incompatible bindings
				if ( derivedBindings.removeIf(
									it -> identifier.test(it) && notCompatible.test(it))) {
								derivedBindings.add(binding);
							}
						}
					}
				});
	}

	@Override
	public String applySorting(Sort sort) {
		return queryEnhancer.applySorting(sort);
	}

	@Override
	public boolean usesJdbcStyleParameters() {
		return usesJdbcStyleParameters;
	}

	@Override
	public String getQueryString() {
		return query;
	}

	public @Nullable String getAlias() {
		return queryEnhancer.detectAlias();
	}

	@Override
	public boolean hasConstructorExpression() {
		return queryEnhancer.hasConstructorExpression();
	}

	@Override
	public boolean isDefaultProjection() {
		return getProjection().equalsIgnoreCase(getAlias());
	}

	@Override
	public boolean hasNamedParameter() {
		return hasNamedParameters;
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
	 * Value object to track and allocate used parameter index labels in a query.
	 */
	static class IndexedParameterLabels {

		private final TreeSet<Integer> usedLabels;
		private final boolean sequential;

		public IndexedParameterLabels(Set<Integer> usedLabels) {

			this.usedLabels = usedLabels instanceof TreeSet<Integer> ts ? ts : new TreeSet<Integer>(usedLabels);
			this.sequential = isSequential(usedLabels);
		}

		private static boolean isSequential(Set<Integer> usedLabels) {

			for (int i = 0; i < usedLabels.size(); i++) {

				if (usedLabels.contains(i + 1)) {
					continue;
				}

				return false;
			}

			return true;
		}

		/**
		 * Allocate the next index label (1-based).
		 *
		 * @return the next index label.
		 */
		public int allocate() {

			if (sequential) {
				int index = usedLabels.size() + 1;
				usedLabels.add(index);

				return index;
			}

			int attempts = usedLabels.last() + 1;
			int index = attemptAllocate(attempts);

			if (index == -1) {
				throw new IllegalStateException(
						"Unable to allocate a unique parameter label. All possible labels have been used.");
			}

			usedLabels.add(index);

			return index;
		}

		private int attemptAllocate(int attempts) {

			for (int i = 0; i < attempts; i++) {

				if (usedLabels.contains(i + 1)) {
					continue;
				}

				return i + 1;
			}

			return -1;
		}

		public boolean hasLabels() {
			return !usedLabels.isEmpty();
		}
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
		private static final Pattern JDBC_STYLE_PARAM = Pattern.compile("(?!\\\\)\\?(?!\\d)"); // no \ and [no digit]
		private static final Pattern NUMBERED_STYLE_PARAM = Pattern.compile("(?!\\\\)\\?\\d"); // no \ and [digit]
		private static final Pattern NAMED_STYLE_PARAM = Pattern.compile("(?!\\\\):\\w+"); // no \ and :[text]

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
		String parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(String query, List<ParameterBinding> bindings,
				Metadata queryMeta) {

			IndexedParameterLabels parameterLabels = new IndexedParameterLabels(findParameterIndices(query));
			boolean parametersShouldBeAccessedByIndex = parameterLabels.hasLabels();

			/*
			 * Prefer indexed access over named parameters if only SpEL Expression parameters are present.
			 */
			if (!parametersShouldBeAccessedByIndex && query.contains("?#{")) {
				parametersShouldBeAccessedByIndex = true;
			}

			ValueExpressionQueryRewriter.ParsedQuery parsedQuery = createSpelExtractor(query,
					parametersShouldBeAccessedByIndex, parameterLabels);

			String resultingQuery = parsedQuery.getQueryString();
			Matcher matcher = PARAMETER_BINDING_PATTERN.matcher(resultingQuery);

			ParameterBindings parameterBindings = new ParameterBindings(bindings, it -> checkAndRegister(it, bindings));
			int currentIndex = 0;

			boolean usesJpaStyleParameters = false;

			while (matcher.find()) {

				if (parsedQuery.isQuoted(matcher.start())) {
					continue;
				}

				String parameterIndexString = matcher.group(INDEXED_PARAMETER_GROUP);
				String parameterName = parameterIndexString != null ? null : matcher.group(NAMED_PARAMETER_GROUP);
				Integer parameterIndex = getParameterIndex(parameterIndexString);

				String match = matcher.group(0);
				if (JDBC_STYLE_PARAM.matcher(match).find()) {
					queryMeta.usesJdbcStyleParameters = true;
				}

				if (NUMBERED_STYLE_PARAM.matcher(match).find() || NAMED_STYLE_PARAM.matcher(match).find()) {
					usesJpaStyleParameters = true;
				}

				if (usesJpaStyleParameters && queryMeta.usesJdbcStyleParameters) {
					throw new IllegalArgumentException("Mixing of ? parameters and other forms like ?1 is not supported");
				}

				String typeSource = matcher.group(COMPARISION_TYPE_GROUP);
				Assert.isTrue(parameterIndexString != null || parameterName != null,
						() -> String.format("We need either a name or an index; Offending query string: %s", query));
				ValueExpression expression = parsedQuery
						.getParameter(parameterName == null ? parameterIndexString : parameterName);
				String replacement = null;

				// this only happens for JDBC-style parameters.
				if ("".equals(parameterIndexString)) {
					parameterIndex = parameterLabels.allocate();
				}

				BindingIdentifier queryParameter;
				if (parameterIndex != null) {
					queryParameter = BindingIdentifier.of(parameterIndex);
				} else if (parameterName != null) {
					queryParameter = BindingIdentifier.of(parameterName);
				} else {
					throw new IllegalStateException("No bindable expression found");
				}
				ParameterOrigin origin = ObjectUtils.isEmpty(expression)
						? ParameterOrigin.ofParameter(parameterName, parameterIndex)
						: ParameterOrigin.ofExpression(expression);

				BindingIdentifier targetBinding = queryParameter;
				Function<BindingIdentifier, ParameterBinding> bindingFactory = switch (ParameterBindingType.of(typeSource)) {
					case LIKE -> {

						Type likeType = LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
						yield (identifier) -> new LikeParameterBinding(identifier, origin, likeType);
					}
					case IN -> (identifier) -> new InParameterBinding(identifier, origin); // fall-through we don't need a special
																																									// parameter queryParameter for the
																																									// given parameter.
					default -> (identifier) -> new ParameterBinding(identifier, origin);
				};

				if (origin.isExpression()) {
					parameterBindings.register(bindingFactory.apply(queryParameter));
				} else {
					targetBinding = parameterBindings.register(queryParameter, origin, bindingFactory, parameterLabels);
				}

				replacement = targetBinding.hasName() ? ":" + targetBinding.getName()
						: ((!usesJpaStyleParameters && queryMeta.usesJdbcStyleParameters) ? "?"
								: "?" + targetBinding.getPosition());
				String result;
				String substring = matcher.group(2);

				int index = resultingQuery.indexOf(substring, currentIndex);
				if (index < 0) {
					result = resultingQuery;
				} else {
					currentIndex = index + replacement.length();
					result = resultingQuery.substring(0, index) + replacement
							+ resultingQuery.substring(index + substring.length());
				}

				resultingQuery = result;
			}

			return resultingQuery;
		}

		private static ValueExpressionQueryRewriter.ParsedQuery createSpelExtractor(String queryWithSpel,
				boolean parametersShouldBeAccessedByIndex, IndexedParameterLabels parameterLabels) {

			/*
			 * If parameters need to be bound by index, we bind the synthetic expression parameters starting from position of the greatest discovered index parameter in order to
			 * not mix-up with the actual parameter indices.
			 */
			BiFunction<Integer, String, String> indexToParameterName = parametersShouldBeAccessedByIndex
					? (index, expression) -> String.valueOf(parameterLabels.allocate())
					: (index, expression) -> EXPRESSION_PARAMETER_PREFIX + (index + 1);

			String fixedPrefix = parametersShouldBeAccessedByIndex ? "?" : ":";

			BiFunction<String, String, String> parameterNameToReplacement = (prefix, name) -> fixedPrefix + name;
			ValueExpressionQueryRewriter rewriter = ValueExpressionQueryRewriter.of(ValueExpressionParser.create(),
					indexToParameterName, parameterNameToReplacement);

			return rewriter.parse(queryWithSpel);
		}

		private static @Nullable Integer getParameterIndex(@Nullable String parameterIndexString) {

			if (parameterIndexString == null || parameterIndexString.isEmpty()) {
				return null;
			}
			return Integer.valueOf(parameterIndexString);
		}

		private static Set<Integer> findParameterIndices(String query) {

			Matcher parameterIndexMatcher = PARAMETER_BINDING_BY_INDEX.matcher(query);
			Set<Integer> usedParameterIndices = new TreeSet<>();

			while (parameterIndexMatcher.find()) {

				String parameterIndexString = parameterIndexMatcher.group(1);
				Integer parameterIndex = getParameterIndex(parameterIndexString);
				if (parameterIndex != null) {
					usedParameterIndices.add(parameterIndex);
				}
			}

			return usedParameterIndices;
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
			public @Nullable String getKeyword() {
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

	static class Metadata {
		private boolean usesJdbcStyleParameters = false;
	}

	/**
	 * Utility to create unique parameter bindings for LIKE that refer to the same underlying method parameter but are
	 * bound to potentially unique query parameters for {@link LikeParameterBinding#prepare(Object) LIKE rewrite}.
	 *
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	static class ParameterBindings {

		private final MultiValueMap<BindingIdentifier, ParameterBinding> methodArgumentToLikeBindings = new LinkedMultiValueMap<>();

		private final Consumer<ParameterBinding> registration;

		public ParameterBindings(List<ParameterBinding> bindings, Consumer<ParameterBinding> registration) {

			for (ParameterBinding binding : bindings) {
				this.methodArgumentToLikeBindings.put(binding.getIdentifier(), new ArrayList<>(List.of(binding)));
			}

			this.registration = registration;
		}

		/**
		 * Return whether the identifier is already bound.
		 *
		 * @param identifier
		 * @return
		 */
		public boolean isBound(BindingIdentifier identifier) {
			return !getBindings(identifier).isEmpty();
		}

		BindingIdentifier register(BindingIdentifier identifier, ParameterOrigin origin,
				Function<BindingIdentifier, ParameterBinding> bindingFactory, IndexedParameterLabels parameterLabels) {

			Assert.isInstanceOf(MethodInvocationArgument.class, origin);

			BindingIdentifier methodArgument = ((MethodInvocationArgument) origin).identifier();
			List<ParameterBinding> bindingsForOrigin = getBindings(methodArgument);

			if (!isBound(identifier)) {

				ParameterBinding binding = bindingFactory.apply(identifier);
				registration.accept(binding);
				bindingsForOrigin.add(binding);
				return binding.getIdentifier();
			}

			ParameterBinding binding = bindingFactory.apply(identifier);

			for (ParameterBinding existing : bindingsForOrigin) {

				if (existing.isCompatibleWith(binding)) {
					return existing.getIdentifier();
				}
			}

			BindingIdentifier syntheticIdentifier;
			if (identifier.hasName() && methodArgument.hasName()) {

				int index = 0;
				String newName = methodArgument.getName();
				while (existsBoundParameter(newName)) {
					index++;
					newName = methodArgument.getName() + "_" + index;
				}
				syntheticIdentifier = BindingIdentifier.of(newName);
			} else {
				syntheticIdentifier = BindingIdentifier.of(parameterLabels.allocate());
			}

			ParameterBinding newBinding = bindingFactory.apply(syntheticIdentifier);
			registration.accept(newBinding);
			bindingsForOrigin.add(newBinding);
			return newBinding.getIdentifier();
		}

		private boolean existsBoundParameter(String key) {
			return methodArgumentToLikeBindings.values().stream().flatMap(Collection::stream)
					.anyMatch(it -> key.equals(it.getName()));
		}

		private List<ParameterBinding> getBindings(BindingIdentifier identifier) {
			return methodArgumentToLikeBindings.computeIfAbsent(identifier, s -> new ArrayList<>());
		}

		public void register(ParameterBinding parameterBinding) {
			registration.accept(parameterBinding);
		}
	}
}
