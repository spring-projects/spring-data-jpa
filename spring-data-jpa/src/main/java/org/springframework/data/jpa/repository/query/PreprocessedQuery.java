/*
 * Copyright 2025 the original author or authors.
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

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.data.expression.ValueExpression;
import org.springframework.data.expression.ValueExpressionParser;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.repository.query.ValueExpressionQueryRewriter;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A pre-parsed query implementing {@link DeclaredQuery} providing information about parameter bindings.
 * <p>
 * Query-preprocessing transforms queries using Spring Data-specific syntax such as {@link TemplatedQuery query
 * templating}, extended {@code LIKE} syntax and usage of {@link ValueExpression value expressions} into a syntax that
 * is valid for JPA queries (JPQL and native).
 * <p>
 * Preprocessing consists of parsing and rewriting so that no extension elements interfere with downstream parsers.
 * However, pre-processing is a lossy procedure because the resulting {@link #getQueryString() query string} only
 * contains parameter binding markers and so the original query cannot be restored. Any query derivation must align its
 * {@link ParameterBinding parameter bindings} to ensure the derived query uses the same binding semantics instead of
 * plain parameters. See {@link ParameterBinding#isCompatibleWith(ParameterBinding)} for further reference.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
final class PreprocessedQuery implements DeclaredQuery {

	private final DeclaredQuery source;
	private final List<ParameterBinding> bindings;
	private final boolean usesJdbcStyleParameters;
	private final boolean containsPageableInSpel;
	private final boolean hasNamedBindings;

	private PreprocessedQuery(DeclaredQuery query, List<ParameterBinding> bindings, boolean usesJdbcStyleParameters,
			boolean containsPageableInSpel) {
		this.source = query;
		this.bindings = bindings;
		this.usesJdbcStyleParameters = usesJdbcStyleParameters;
		this.containsPageableInSpel = containsPageableInSpel;
		this.hasNamedBindings = containsNamedParameter(bindings);
	}

	private static boolean containsNamedParameter(List<ParameterBinding> bindings) {

		for (ParameterBinding parameterBinding : bindings) {
			if (parameterBinding.getIdentifier().hasName() && parameterBinding.getOrigin()
					.isMethodArgument()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parse a {@link DeclaredQuery query} into its parametrized form by identifying anonymous, named, indexed and SpEL
	 * parameters. Query parsing applies special treatment to {@code IN} and {@code LIKE} parameter bindings.
	 *
	 * @param declaredQuery the source query to parse.
	 * @return a parsed {@link PreprocessedQuery}.
	 */
	public static PreprocessedQuery parse(DeclaredQuery declaredQuery) {
		return ParameterBindingParser.INSTANCE.parse(declaredQuery.getQueryString(), declaredQuery::rewrite,
				parameterBindings -> {
				});
	}

	@Override
	public String getQueryString() {
		return source.getQueryString();
	}

	@Override
	public boolean isNative() {
		return source.isNative();
	}

	boolean hasBindings() {
		return !bindings.isEmpty();
	}

	boolean hasNamedBindings() {
		return this.hasNamedBindings;
	}

	boolean containsPageableInSpel() {
		return containsPageableInSpel;
	}

	boolean usesJdbcStyleParameters() {
		return usesJdbcStyleParameters;
	}

	List<ParameterBinding> getBindings() {
		return Collections.unmodifiableList(bindings);
	}

	/**
	 * Derive a query (typically a count query) from the given query string. We need to copy expression bindings from the
	 * declared to the derived query as JPQL query derivation only sees JPA parameter markers and not the original
	 * expressions anymore.
	 *
	 * @return
	 */
	@Override
	public PreprocessedQuery rewrite(String newQueryString) {

		return ParameterBindingParser.INSTANCE.parse(newQueryString, source::rewrite, derivedBindings -> {

			// need to copy expression bindings from the declared to the derived query as JPQL query derivation only sees
			// JPA parameter markers and not the original expressions anymore.
			if (this.hasBindings() && !this.bindings.equals(derivedBindings)) {

				for (ParameterBinding binding : bindings) {

					Predicate<ParameterBinding> identifier = binding::bindsTo;
					Predicate<ParameterBinding> notCompatible = Predicate.not(binding::isCompatibleWith);

					// replace incompatible bindings
					if (derivedBindings.removeIf(it -> identifier.test(it) && notCompatible.test(it))) {
						derivedBindings.add(binding);
					}
				}
			}
		});
	}

	@Override
	public String toString() {
		return "ParametrizedQuery[" + source + ", " + bindings + ']';
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
		PreprocessedQuery parse(String query, Function<String, DeclaredQuery> declaredQueryFactory,
				Consumer<List<ParameterBinding>> parameterBindingPostProcessor) {

			IndexedParameterLabels parameterLabels = new IndexedParameterLabels(findParameterIndices(query));
			boolean parametersShouldBeAccessedByIndex = parameterLabels.hasLabels();

			List<ParameterBinding> bindings = new ArrayList<>();
			boolean jdbcStyle = false;
			boolean containsPageableInSpel = query.contains("#pageable");

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
					jdbcStyle = true;
				}

				if (NUMBERED_STYLE_PARAM.matcher(match)
							.find() || NAMED_STYLE_PARAM.matcher(match).find()) {
					usesJpaStyleParameters = true;
				}

				if (usesJpaStyleParameters && jdbcStyle) {
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

				ParameterBinding.BindingIdentifier queryParameter;
				if (parameterIndex != null) {
					queryParameter = ParameterBinding.BindingIdentifier.of(parameterIndex);
				}
				else if (parameterName != null) {
					queryParameter = ParameterBinding.BindingIdentifier.of(parameterName);
				}
				else {
					throw new IllegalStateException("No bindable expression found");
				}
				ParameterBinding.ParameterOrigin origin = ObjectUtils.isEmpty(expression)
						? ParameterBinding.ParameterOrigin.ofParameter(parameterName, parameterIndex)
						: ParameterBinding.ParameterOrigin.ofExpression(expression);

				ParameterBinding.BindingIdentifier targetBinding = queryParameter;
				Function<ParameterBinding.BindingIdentifier, ParameterBinding> bindingFactory = switch (ParameterBindingType
						.of(typeSource)) {
					case LIKE -> {

						Part.Type likeType = ParameterBinding.LikeParameterBinding.getLikeTypeFrom(matcher.group(2));
						yield (identifier) -> new ParameterBinding.LikeParameterBinding(identifier, origin, likeType);
					}
					case IN ->
							(identifier) -> new ParameterBinding.InParameterBinding(identifier, origin); // fall-through we
					// don't need a special
					// parameter queryParameter for the
					// given parameter.
					default -> (identifier) -> new ParameterBinding(identifier, origin);
				};

				if (origin.isExpression()) {
					parameterBindings.register(bindingFactory.apply(queryParameter));
				}
				else {
					targetBinding = parameterBindings.register(queryParameter, origin, bindingFactory, parameterLabels);
				}

				replacement = targetBinding.hasName() ? ":" + targetBinding.getName()
						: ((!usesJpaStyleParameters && jdbcStyle) ? "?" : "?" + targetBinding.getPosition());
				String result;
				String substring = matcher.group(2);

				int index = resultingQuery.indexOf(substring, currentIndex);
				if (index < 0) {
					result = resultingQuery;
				}
				else {
					currentIndex = index + replacement.length();
					result = resultingQuery.substring(0, index) + replacement
							 + resultingQuery.substring(index + substring.length());
				}

				resultingQuery = result;
			}

			parameterBindingPostProcessor.accept(bindings);
			return new PreprocessedQuery(declaredQueryFactory.apply(resultingQuery), bindings, jdbcStyle,
					containsPageableInSpel);
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

	/**
	 * Utility to create unique parameter bindings for LIKE that refer to the same underlying method parameter but are
	 * bound to potentially unique query parameters for {@link ParameterBinding.LikeParameterBinding#prepare(Object) LIKE
	 * rewrite}.
	 *
	 * @author Mark Paluch
	 * @since 3.1.2
	 */
	private static class ParameterBindings {

		private final MultiValueMap<BindingIdentifier, ParameterBinding> methodArgumentToLikeBindings = new LinkedMultiValueMap<>();

		private final Consumer<ParameterBinding> registration;

		public ParameterBindings(List<ParameterBinding> bindings, Consumer<ParameterBinding> registration) {

			for (ParameterBinding binding : bindings) {
				this.methodArgumentToLikeBindings.put(binding.getIdentifier(), new ArrayList<>(List.of(binding)));
			}

			this.registration = registration;
		}

		/**
		 * @param identifier
		 * @return whether the identifier is already bound.
		 */
		public boolean isBound(ParameterBinding.BindingIdentifier identifier) {
			return !getBindings(identifier).isEmpty();
		}

		ParameterBinding.BindingIdentifier register(ParameterBinding.BindingIdentifier identifier,
				ParameterBinding.ParameterOrigin origin,
				Function<ParameterBinding.BindingIdentifier, ParameterBinding> bindingFactory,
				IndexedParameterLabels parameterLabels) {

			Assert.isInstanceOf(ParameterBinding.MethodInvocationArgument.class, origin);

			ParameterBinding.BindingIdentifier methodArgument = ((ParameterBinding.MethodInvocationArgument) origin)
					.identifier();
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

			ParameterBinding.BindingIdentifier syntheticIdentifier;
			if (identifier.hasName() && methodArgument.hasName()) {

				int index = 0;
				String newName = methodArgument.getName();
				while (existsBoundParameter(newName)) {
					index++;
					newName = methodArgument.getName() + "_" + index;
				}
				syntheticIdentifier = ParameterBinding.BindingIdentifier.of(newName);
			}
			else {
				syntheticIdentifier = ParameterBinding.BindingIdentifier.of(parameterLabels.allocate());
			}

			ParameterBinding newBinding = bindingFactory.apply(syntheticIdentifier);
			registration.accept(newBinding);
			bindingsForOrigin.add(newBinding);
			return newBinding.getIdentifier();
		}

		private boolean existsBoundParameter(String key) {
			return methodArgumentToLikeBindings.values().stream()
					.flatMap(Collection::stream)
					.anyMatch(it -> key.equals(it.getName()));
		}

		private List<ParameterBinding> getBindings(ParameterBinding.BindingIdentifier identifier) {
			return methodArgumentToLikeBindings.computeIfAbsent(identifier, s -> new ArrayList<>());
		}

		public void register(ParameterBinding parameterBinding) {
			registration.accept(parameterBinding);
		}
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

}
