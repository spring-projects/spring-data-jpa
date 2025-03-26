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
package org.springframework.data.jpa.repository.aot.generated;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.QueryHint;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Common code blocks for JPA AOT Fragment generation.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class JpaCodeBlocks {

	/**
	 * @param context
	 * @return new {@link QueryBlockBuilder}.
	 */
	public static QueryBlockBuilder queryBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {
		return new QueryBlockBuilder(context, queryMethod);
	}

	/**
	 * @param context
	 * @return new {@link QueryExecutionBlockBuilder}.
	 */
	static QueryExecutionBlockBuilder executionBuilder(AotQueryMethodGenerationContext context,
			JpaQueryMethod queryMethod) {
		return new QueryExecutionBlockBuilder(context, queryMethod);
	}

	/**
	 * Builder for the actual query code block.
	 */
	static class QueryBlockBuilder {

		private final AotQueryMethodGenerationContext context;
		private final JpaQueryMethod queryMethod;
		private String queryVariableName = "query";
		private AotQueries queries;
		private MergedAnnotation<QueryHints> queryHints = MergedAnnotation.missing();
		private MergedAnnotation<org.springframework.data.jpa.repository.Query> query = MergedAnnotation.missing();
		private @Nullable String sqlResultSetMapping;
		private @Nullable Class<?> queryReturnType;

		private QueryBlockBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {
			this.context = context;
			this.queryMethod = queryMethod;
		}

		public QueryBlockBuilder usingQueryVariableName(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		public QueryBlockBuilder filter(AotQueries query) {
			this.queries = query;
			return this;
		}

		public QueryBlockBuilder queryHints(MergedAnnotation<QueryHints> queryHints) {

			this.queryHints = queryHints;
			return this;
		}

		public QueryBlockBuilder query(MergedAnnotation<org.springframework.data.jpa.repository.Query> query) {

			this.query = query;
			return this;
		}

		public QueryBlockBuilder nativeQuery(MergedAnnotation<NativeQuery> nativeQuery) {

			if (nativeQuery.isPresent()) {
				this.sqlResultSetMapping = nativeQuery.getString("sqlResultSetMapping");
			}
			return this;
		}

		public QueryBlockBuilder queryReturnType(@Nullable Class<?> queryReturnType) {
			this.queryReturnType = queryReturnType;
			return this;
		}

		/**
		 * Build the query block.
		 *
		 * @return
		 */
		public CodeBlock build() {

			boolean isProjecting = context.getReturnedType().isProjecting();
			Class<?> actualReturnType = isProjecting ? context.getActualReturnType().toClass()
					: context.getRepositoryInformation().getDomainType();

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");

			String queryStringNameVariableName = null;

			if (queries.result() instanceof StringAotQuery sq) {

				queryStringNameVariableName = "%sString".formatted(queryVariableName);
				builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, sq.getQueryString());
			}

			String countQueryStringNameVariableName = null;
			String countQueryVariableName = "count%s".formatted(StringUtils.capitalize(queryVariableName));

			if (queryMethod.isPageQuery() && queries.count() instanceof StringAotQuery sq) {

				countQueryStringNameVariableName = "count%sString".formatted(StringUtils.capitalize(queryVariableName));
				builder.addStatement("$T $L = $S", String.class, countQueryStringNameVariableName, sq.getQueryString());
			}

			// sorting
			// TODO: refactor into sort builder

			String sortParameterName = context.getSortParameterName();
			if (sortParameterName == null && context.getPageableParameterName() != null) {
				sortParameterName = "%s.getSort()".formatted(context.getPageableParameterName());
			}

			if (StringUtils.hasText(sortParameterName) && queries.result() instanceof StringAotQuery) {
				builder.add(applySorting(sortParameterName, queryStringNameVariableName, actualReturnType));
			}

			if (queries.result().hasExpression() || queries.count().hasExpression()) {
				builder.addStatement("class ExpressionMarker{}");
			}

			builder.add(createQuery(queryVariableName, queryStringNameVariableName, queries.result(),
					this.sqlResultSetMapping, this.queryHints, this.queryReturnType));

			builder.add(applyLimits(queries.result().isExists()));

			if (queryMethod.isPageQuery()) {

				builder.beginControlFlow("$T $L = () ->", LongSupplier.class, "countAll");

				boolean queryHints = this.queryHints.isPresent() && this.queryHints.getBoolean("forCounting");

				builder.add(createQuery(countQueryVariableName, countQueryStringNameVariableName, queries.count(), null,
						queryHints ? this.queryHints : MergedAnnotation.missing(), Long.class));
				builder.addStatement("return ($T) $L.getSingleResult()", Long.class, countQueryVariableName);

				// end control flow does not work well with lambdas
				builder.unindent();
				builder.add("};\n");
			}

			return builder.build();
		}

		private CodeBlock applySorting(String sort, String queryString, Class<?> actualReturnType) {

			Builder builder = CodeBlock.builder();
			builder.beginControlFlow("if ($L.isSorted())", sort);

			builder.addStatement("$T declaredQuery = $T.$L($L)", DeclaredQuery.class, DeclaredQuery.class,
					queries.isNative() ? "nativeQuery" : "jpqlQuery",
						queryString);

			builder.addStatement("$L = rewriteQuery(declaredQuery, $L, $T.class)", queryString, sort, actualReturnType);
			builder.endControlFlow();

			return builder.build();
		}

		private CodeBlock applyLimits(boolean exists) {

			Builder builder = CodeBlock.builder();

			if (exists) {
				builder.addStatement("$L.setMaxResults(1)", queryVariableName);

				return builder.build();
			}

			String limit = context.getLimitParameterName();

			if (StringUtils.hasText(limit)) {
				builder.beginControlFlow("if ($L.isLimited())", limit);
				builder.addStatement("$L.setMaxResults($L.max())", queryVariableName, limit);
				builder.endControlFlow();
			} else if (queries.result().isLimited()) {
				builder.addStatement("$L.setMaxResults($L)", queryVariableName, queries.result().getLimit().max());
			}

			String pageable = context.getPageableParameterName();

			if (StringUtils.hasText(pageable)) {

				builder.beginControlFlow("if ($L.isPaged())", pageable);
				builder.addStatement("$L.setFirstResult(Long.valueOf($L.getOffset()).intValue())", queryVariableName, pageable);
				if (queryMethod.isSliceQuery()) {
					builder.addStatement("$L.setMaxResults($L.getPageSize() + 1)", queryVariableName, pageable);
				} else {
					builder.addStatement("$L.setMaxResults($L.getPageSize())", queryVariableName, pageable);
				}
				builder.endControlFlow();
			}

			return builder.build();
		}

		private CodeBlock createQuery(String queryVariableName, @Nullable String queryStringNameVariableName,
				AotQuery query, @Nullable String sqlResultSetMapping, MergedAnnotation<QueryHints> queryHints,
				@Nullable Class<?> queryReturnType) {

			Builder builder = CodeBlock.builder();

			builder.add(
					doCreateQuery(queryVariableName, queryStringNameVariableName, query, sqlResultSetMapping, queryReturnType));

			if (queryHints.isPresent()) {
				builder.add(applyHints(queryVariableName, queryHints));
				builder.add("\n");
			}

			for (ParameterBinding binding : query.getParameterBindings()) {

				Object prepare = binding.prepare("s");
				Object parameterIdentifier = getParameterName(binding.getIdentifier());
				String valueFormat = parameterIdentifier instanceof CharSequence ? "$S" : "$L";

				if (prepare instanceof String prepared && !prepared.equals("s")) {

					String format = prepared.replaceAll("%", "%%").replace("s", "%s");
					builder.addStatement("$L.setParameter(%s, $S.formatted($L))".formatted(valueFormat), queryVariableName,
							parameterIdentifier, format, getParameter(binding.getOrigin()));
				} else {
					builder.addStatement("$L.setParameter(%s, $L)".formatted(valueFormat), queryVariableName, parameterIdentifier,
							getParameter(binding.getOrigin()));
				}
			}

			return builder.build();
		}

		private CodeBlock doCreateQuery(String queryVariableName, @Nullable String queryStringNameVariableName,
				AotQuery query, @Nullable String sqlResultSetMapping, @Nullable Class<?> queryReturnType) {

			Builder builder = CodeBlock.builder();

			if (query instanceof StringAotQuery) {

				if (StringUtils.hasText(sqlResultSetMapping)) {

					builder.addStatement("$T $L = this.$L.createNativeQuery($L, $S)", Query.class, queryVariableName,
							context.fieldNameOf(EntityManager.class), queryStringNameVariableName, sqlResultSetMapping);

					return builder.build();
				}

				if (query.isNative() && queryReturnType != null) {

					builder.addStatement("$T $L = this.$L.createNativeQuery($L, $T.class)", Query.class, queryVariableName,
							context.fieldNameOf(EntityManager.class), queryStringNameVariableName, queryReturnType);

					return builder.build();
				}

				builder.addStatement("$T $L = this.$L.$L($L)", Query.class, queryVariableName,
						context.fieldNameOf(EntityManager.class), query.isNative() ? "createNativeQuery" : "createQuery",
						queryStringNameVariableName);

				return builder.build();
			}

			if (query instanceof NamedAotQuery nq) {

				if (queryReturnType != null) {

					builder.addStatement("$T $L = this.$L.createNamedQuery($S, $T.class)", Query.class, queryVariableName,
							context.fieldNameOf(EntityManager.class), nq.getName(), queryReturnType);

					return builder.build();
				}

				builder.addStatement("$T $L = this.$L.createNamedQuery($S)", Query.class, queryVariableName,
						context.fieldNameOf(EntityManager.class), nq.getName());

				return builder.build();
			}

			throw new UnsupportedOperationException("Unsupported query type: " + query);
		}

		private Object getParameterName(ParameterBinding.BindingIdentifier identifier) {

			if (identifier.hasPosition()) {
				return identifier.getPosition();
			}

			return identifier.getName();

		}

		private Object getParameter(ParameterBinding.ParameterOrigin origin) {

			if (origin.isMethodArgument() && origin instanceof ParameterBinding.MethodInvocationArgument mia) {

				if (mia.identifier().hasPosition()) {
					return context.getRequiredBindableParameterName(mia.identifier().getPosition() - 1);
				}

				if (mia.identifier().hasName()) {
					return context.getRequiredBindableParameterName(mia.identifier().getName());
				}
			}

			if (origin.isExpression() && origin instanceof ParameterBinding.Expression expr) {

				Builder builder = CodeBlock.builder();
				ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
				String[] parameterNames = discoverer.getParameterNames(context.getMethod());

				String expressionString = expr.expression().getExpressionString();
				// re-wrap expression
				if (!expressionString.startsWith("$")) {
					expressionString = "#{" + expressionString + "}";
				}

				builder.add("evaluateExpression(ExpressionMarker.class.getEnclosingMethod(), $S, $L)", expressionString,
						StringUtils.arrayToCommaDelimitedString(parameterNames));

				return builder.build();
			}

			throw new UnsupportedOperationException("Not supported yet");
		}

		private CodeBlock applyHints(String queryVariableName, MergedAnnotation<QueryHints> queryHints) {

			Builder hintsBuilder = CodeBlock.builder();
			MergedAnnotation<QueryHint>[] values = queryHints.getAnnotationArray("value", QueryHint.class);

			for (MergedAnnotation<QueryHint> hint : values) {
				hintsBuilder.addStatement("$L.setHint($S, $S)", queryVariableName, hint.getString("name"),
						hint.getString("value"));
			}

			return hintsBuilder.build();
		}

	}

	static class QueryExecutionBlockBuilder {

		private final AotQueryMethodGenerationContext context;
		private final JpaQueryMethod queryMethod;
		private @Nullable AotQuery aotQuery;
		private String queryVariableName = "query";
		private MergedAnnotation<Modifying> modifying = MergedAnnotation.missing();

		private QueryExecutionBlockBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {
			this.context = context;
			this.queryMethod = queryMethod;
		}

		public QueryExecutionBlockBuilder referencing(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		public QueryExecutionBlockBuilder query(AotQuery aotQuery) {

			this.aotQuery = aotQuery;
			return this;
		}

		public QueryExecutionBlockBuilder modifying(MergedAnnotation<Modifying> modifying) {

			this.modifying = modifying;
			return this;
		}

		public CodeBlock build() {

			Builder builder = CodeBlock.builder();

			boolean isProjecting = context.getActualReturnType() != null
					&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
							context.getActualReturnType());
			Type actualReturnType = isProjecting ? context.getActualReturnType().getType()
					: context.getRepositoryInformation().getDomainType();
			builder.add("\n");

			if (modifying.isPresent()) {

				if (modifying.getBoolean("flushAutomatically")) {
					builder.addStatement("this.$L.flush()", context.fieldNameOf(EntityManager.class));
				}

				Class<?> returnType = context.getMethod().getReturnType();

				if (returnsModifying(returnType)) {
					builder.addStatement("int result = $L.executeUpdate()", queryVariableName);
				} else {
					builder.addStatement("$L.executeUpdate()", queryVariableName);
				}

				if (modifying.getBoolean("clearAutomatically")) {
					builder.addStatement("this.$L.clear()", context.fieldNameOf(EntityManager.class));
				}

				if (returnType == int.class || returnType == long.class || returnType == Integer.class) {
					builder.addStatement("return result");
				}

				if (returnType == Long.class) {
					builder.addStatement("return (long) result");
				}

				return builder.build();
			}

			if (aotQuery != null && aotQuery.isDelete()) {

				builder.addStatement("$T<$T> resultList = $L.getResultList()", List.class, actualReturnType, queryVariableName);
				builder.addStatement("resultList.forEach($L::remove)", context.fieldNameOf(EntityManager.class));
				if (!context.getReturnType().isAssignableFrom(List.class)) {
					if (ClassUtils.isAssignable(Number.class, context.getMethod().getReturnType())) {
						builder.addStatement("return $T.valueOf(resultList.size())", context.getMethod().getReturnType());
					} else {
						builder.addStatement("return resultList.isEmpty() ? null : resultList.iterator().next()");
					}
				} else {
					builder.addStatement("return resultList");
				}
			} else if (aotQuery != null && aotQuery.isExists()) {
				builder.addStatement("return !$L.getResultList().isEmpty()", queryVariableName);
			} else {

				if (queryMethod.isCollectionQuery()) {
					builder.addStatement("return ($T) query.getResultList()", context.getReturnTypeName());
				} else if (queryMethod.isStreamQuery()) {
					builder.addStatement("return ($T) query.getResultStream()", context.getReturnTypeName());
				} else if (queryMethod.isPageQuery()) {
					builder.addStatement("return $T.getPage(($T<$T>) $L.getResultList(), $L, countAll)",
							PageableExecutionUtils.class, List.class, actualReturnType, queryVariableName,
							context.getPageableParameterName());
				} else if (queryMethod.isSliceQuery()) {
					builder.addStatement("$T<$T> resultList = $L.getResultList()", List.class, actualReturnType,
							queryVariableName);
					builder.addStatement("boolean hasNext = $L.isPaged() && resultList.size() > $L.getPageSize()",
							context.getPageableParameterName(), context.getPageableParameterName());
					builder.addStatement(
							"return new $T<>(hasNext ? resultList.subList(0, $L.getPageSize()) : resultList, $L, hasNext)",
							SliceImpl.class, context.getPageableParameterName(), context.getPageableParameterName());
				} else {

					if (Optional.class.isAssignableFrom(context.getReturnType().toClass())) {
						builder.addStatement("return $T.ofNullable(($T) $L.getSingleResultOrNull())", Optional.class,
								actualReturnType, queryVariableName);
					} else {
						builder.addStatement("return ($T) $L.getSingleResultOrNull()", context.getReturnTypeName(),
								queryVariableName);
					}
				}
			}

			return builder.build();
		}

		public static boolean returnsModifying(Class<?> returnType) {

			return returnType == int.class || returnType == long.class || returnType == Integer.class
					|| returnType == Long.class;
		}

	}


}
