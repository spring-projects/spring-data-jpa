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
package org.springframework.data.jpa.repository.aot;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Tuple;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.LongSupplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Vector;
import org.springframework.data.javapoet.LordOfTheStrings;
import org.springframework.data.javapoet.TypeNames;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotQueryMethodGenerationContext;
import org.springframework.data.repository.aot.generate.MethodReturn;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.Streamable;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.util.Assert;
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
	 * @return new {@link QueryBlockBuilder}.
	 */
	public static QueryBlockBuilder queryBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {
		return new QueryBlockBuilder(context, queryMethod);
	}

	/**
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
		private final String parameterNames;
		private final String queryVariableName;
		private @Nullable AotQueries queries;
		private MergedAnnotation<QueryHints> queryHints = MergedAnnotation.missing();
		private @Nullable AotEntityGraph entityGraph;
		private @Nullable String sqlResultSetMapping;
		private @Nullable Class<?> queryReturnType;
		private @Nullable Class<?> queryRewriter = QueryRewriter.IdentityQueryRewriter.class;

		private QueryBlockBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {

			this.context = context;
			this.queryMethod = queryMethod;
			this.queryVariableName = context.localVariable("query");

			String parameterNames = StringUtils.collectionToDelimitedString(context.getAllParameterNames(), ", ");

			if (StringUtils.hasText(parameterNames)) {
				this.parameterNames = ", " + parameterNames;
			} else {
				this.parameterNames = "";
			}
		}

		public QueryBlockBuilder filter(AotQueries query) {
			this.queries = query;
			return this;
		}

		public QueryBlockBuilder nativeQuery(MergedAnnotation<NativeQuery> nativeQuery) {

			if (nativeQuery.isPresent()) {
				this.sqlResultSetMapping = nativeQuery.getString("sqlResultSetMapping");
			}
			return this;
		}

		public QueryBlockBuilder queryHints(MergedAnnotation<QueryHints> queryHints) {

			this.queryHints = queryHints;
			return this;
		}

		public QueryBlockBuilder entityGraph(@Nullable AotEntityGraph entityGraph) {
			this.entityGraph = entityGraph;
			return this;
		}

		public QueryBlockBuilder queryReturnType(@Nullable Class<?> queryReturnType) {
			this.queryReturnType = queryReturnType;
			return this;
		}

		public QueryBlockBuilder queryRewriter(@Nullable Class<?> queryRewriter) {
			this.queryRewriter = queryRewriter == null ? QueryRewriter.IdentityQueryRewriter.class : queryRewriter;
			return this;
		}

		/**
		 * Build the query block.
		 *
		 * @return
		 */
		public CodeBlock build() {

			Assert.notNull(queries, "Queries must not be null");

			MethodReturn methodReturn = context.getMethodReturn();
			boolean isProjecting = methodReturn.isProjecting();

			String dynamicReturnType = null;
			if (queryMethod.getParameters().hasDynamicProjection()) {
				dynamicReturnType = context.getParameterName(queryMethod.getParameters().getDynamicProjectionIndex());
			}

			CodeBlock.Builder builder = CodeBlock.builder();

			String queryStringVariableName = null;
			String queryRewriterName = null;

			if (queries.result() instanceof StringAotQuery && queryRewriter != QueryRewriter.IdentityQueryRewriter.class) {

				queryRewriterName = context.localVariable("queryRewriter");
				builder.addStatement("$T $L = new $T()", queryRewriter, queryRewriterName, queryRewriter);
			}

			if (queries.result() instanceof StringAotQuery sq) {

				queryStringVariableName = "%sString".formatted(queryVariableName);
				builder.add(buildQueryString(sq, queryStringVariableName));
			}

			String countQueryStringNameVariableName = null;
			String countQueryVariableName = context
					.localVariable("count%s".formatted(StringUtils.capitalize(queryVariableName)));

			if (queryMethod.isPageQuery() && queries.count() instanceof StringAotQuery sq) {

				countQueryStringNameVariableName = context
						.localVariable("count%sString".formatted(StringUtils.capitalize(queryVariableName)));
				builder.add(buildQueryString(sq, countQueryStringNameVariableName));
			}
			String pageable = context.getPageableParameterName();

			if (pageable != null) {
				String pageableVariableName = context.localVariable("pageable");
				builder.addStatement("$1T $2L = $3L != null ? $3L : $1T.unpaged()", Pageable.class, pageableVariableName,
						pageable);
				pageable = pageableVariableName;
			}

			String sortParameterName = context.getSortParameterName();
			if (sortParameterName == null && pageable != null) {
				sortParameterName = "%s.getSort()".formatted(pageable);
			}

			if ((StringUtils.hasText(sortParameterName) || StringUtils.hasText(dynamicReturnType))
					&& queries != null && queries.result() instanceof StringAotQuery
					&& StringUtils.hasText(queryStringVariableName)) {
				builder.add(applyRewrite(sortParameterName, dynamicReturnType, isProjecting, queryStringVariableName));
			}

			builder.add(createQuery(false, queryVariableName, queryStringVariableName, queryRewriterName, queries.result(),
					this.sqlResultSetMapping, pageable, this.queryHints, this.entityGraph, this.queryReturnType));

			builder.add(applyLimits(queries.result().isExists(), pageable));

			if (queryMethod.isPageQuery()) {

				builder.beginControlFlow("$T $L = () ->", LongSupplier.class, context.localVariable("countAll"));

				boolean queryHints = this.queryHints.isPresent() && this.queryHints.getBoolean("forCounting");

				builder.add(createQuery(true, countQueryVariableName, countQueryStringNameVariableName, queryRewriterName,
						queries.count(), null, pageable,
						queryHints ? this.queryHints : MergedAnnotation.missing(), null, Long.class));
				builder.addStatement("return getCount($L)", countQueryVariableName);

				// end control flow does not work well with lambdas
				builder.unindent();
				builder.add("};\n");
			}

			return builder.build();
		}

		private CodeBlock buildQueryString(StringAotQuery sq, String queryStringVariableName) {

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.addStatement("$T $L = $S", String.class, queryStringVariableName, sq.getQueryString());
			return builder.build();
		}

		private CodeBlock applyRewrite(@Nullable String sort, @Nullable String dynamicReturnType, boolean isProjecting,
				String queryString) {

			Builder builder = CodeBlock.builder();

			boolean hasSort = StringUtils.hasText(sort);
			if (hasSort) {
				builder.beginControlFlow("if ($L.isSorted())", sort);
			}

			builder.addStatement("$T $L = $T.$L($L)", DeclaredQuery.class, context.localVariable("declaredQuery"),
					DeclaredQuery.class,
					queries != null && queries.isNative() ? "nativeQuery" : "jpqlQuery", queryString);

			boolean hasDynamicReturnType = StringUtils.hasText(dynamicReturnType);

			if (hasSort && hasDynamicReturnType) {
				builder.addStatement("$L = rewriteQuery($L, $L, $L)", queryString, context.localVariable("declaredQuery"), sort,
						dynamicReturnType);
			} else if (hasSort) {

				Object actualReturnType = isProjecting ? context.getMethodReturn().getActualClassName()
						: context.getDomainType();

				builder.addStatement("$L = rewriteQuery($L, $L, $T.class)", queryString, context.localVariable("declaredQuery"),
						sort, actualReturnType);
			} else if (hasDynamicReturnType) {
				builder.addStatement("$L = rewriteQuery($L, $T.unsorted(), $L)", context.localVariable("declaredQuery"),
						queryString, Sort.class,
						dynamicReturnType);
			}

			if (hasSort) {
				builder.endControlFlow();
			}

			return builder.build();
		}

		private CodeBlock applyLimits(boolean exists, @Nullable String pageable) {

			Assert.notNull(queries, "Queries must not be null");

			Builder builder = CodeBlock.builder();

			if (exists) {
				builder.addStatement("$L.setMaxResults(1)", queryVariableName);
				return builder.build();
			}

			if (queries != null && queries.result() instanceof StringAotQuery sq && sq.hasPagingExpression()) {
				return builder.build();
			}

			String limit = context.getLimitParameterName();

			if (StringUtils.hasText(limit)) {
				builder.beginControlFlow("if ($L.isLimited())", limit);
				builder.addStatement("$L.setMaxResults($L.max())", queryVariableName, limit);
				builder.endControlFlow();
			}

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

			if (queries.result().isLimited()) {

				int max = queries.result().getLimit().max();

				builder.beginControlFlow("if ($L.getMaxResults() != $T.MAX_VALUE)", queryVariableName, Integer.class);
				builder.beginControlFlow("if ($1L.getMaxResults() > $2L && $1L.getFirstResult() > 0)", queryVariableName, max);
				builder.addStatement("$1L.setFirstResult($1L.getFirstResult() - ($1L.getMaxResults() - $2L))",
						queryVariableName, max);
				builder.endControlFlow();
				builder.endControlFlow();

				builder.addStatement("$1L.setMaxResults($2L)", queryVariableName, max);
			}

			return builder.build();
		}

		private CodeBlock createQuery(boolean count, String queryVariableName, @Nullable String queryStringNameVariableName,
				@Nullable String queryRewriterName, AotQuery query, @Nullable String sqlResultSetMapping,
				@Nullable String pageable,
				MergedAnnotation<QueryHints> queryHints,
				@Nullable AotEntityGraph entityGraph, @Nullable Class<?> queryReturnType) {

			Builder builder = CodeBlock.builder();

			builder.add(doCreateQuery(count, queryVariableName, queryStringNameVariableName, queryRewriterName, query,
					sqlResultSetMapping, pageable,
					queryReturnType));

			if (entityGraph != null) {
				builder.add(applyEntityGraph(entityGraph, queryVariableName));
			}

			if (queryHints.isPresent()) {
				builder.add(applyHints(queryVariableName, queryHints));
				builder.add("\n");
			}

			for (ParameterBinding binding : query.getParameterBindings()) {

				Object prepare = binding.prepare("s");
				Object parameterIdentifier = getParameterName(binding.getIdentifier());
				String valueFormat = parameterIdentifier instanceof CharSequence ? "$S" : "$L";

				Object parameter = getParameter(binding.getOrigin());

				if (parameter instanceof String parameterName) {
					MethodParameter methodParameter = context.getMethodParameter(parameterName);
					if (methodParameter != null) {
						parameter = postProcessBindingValue(binding, methodParameter, parameterName);
					}
				}

				if (prepare instanceof String prepared && !prepared.equals("s")) {

					String format = prepared.replaceAll("%", "%%").replace("s", "%s");
					builder.addStatement("$L.setParameter(%s, $S.formatted($L))".formatted(valueFormat), queryVariableName,
							parameterIdentifier, format, parameter);
				} else {
					builder.addStatement("$L.setParameter(%s, $L)".formatted(valueFormat), queryVariableName, parameterIdentifier,
							parameter);
				}
			}

			return builder.build();
		}

		private Object postProcessBindingValue(ParameterBinding binding, MethodParameter methodParameter,
				String parameterName) {

			Class<?> parameterType = methodParameter.getParameterType();
			if (Score.class.isAssignableFrom(parameterType)) {
				return parameterName + ".getValue()";
			}

			if (Vector.class.isAssignableFrom(parameterType)) {
				return "%1$s.getType() == Float.TYPE ? %1$s.toFloatArray() : %1$s.toDoubleArray()".formatted(parameterName);
			}

			if (binding instanceof ParameterBinding.PartTreeParameterBinding treeBinding) {

				if (treeBinding.isIgnoreCase()) {

					String function = treeBinding.getTemplates() == JpqlQueryTemplates.LOWER ? "toLowerCase" : "toUpperCase";

					if (isArray(parameterType) || Collection.class.isAssignableFrom(parameterType)) {
						return CodeBlock.builder().add("mapIgnoreCase($L, $T::$L)", parameterName, String.class, function).build();
					}

					if (String.class.isAssignableFrom(parameterType)) {
						return "%1$s != null ? %1$s.%2$s() : %1$s".formatted(parameterName, function);
					}

					return "%1$s != null ? %1$s.toString().%2$s() : %1$s".formatted(parameterName, function);
				}
			}

			if (isArray(parameterType)) {
				return CodeBlock.builder().add("$T.asList($L)", Arrays.class, parameterName).build();
			}

			return parameterName;
		}

		private static boolean isArray(Class<?> parameterType) {
			return parameterType.isArray() && !parameterType.getComponentType().equals(byte.class)
					&& !parameterType.getComponentType().equals(Byte.class);
		}

		private CodeBlock doCreateQuery(boolean count, String queryVariableName,
				@Nullable String queryStringName, @Nullable String queryRewriterName, AotQuery query,
				@Nullable String sqlResultSetMapping,
				@Nullable String pageable,
				@Nullable Class<?> queryReturnType) {

			MethodReturn methodReturn = context.getMethodReturn();
			Builder builder = CodeBlock.builder();
			String queryStringNameToUse = queryStringName;

			if (query instanceof StringAotQuery sq) {

				if (StringUtils.hasText(queryRewriterName)) {

					queryStringNameToUse = queryStringName + "Rewritten";

					if (StringUtils.hasText(pageable)) {
						builder.addStatement("$T $L = $L.rewrite($L, $L)", String.class, queryStringNameToUse, queryRewriterName,
								queryStringName, pageable);
					} else if (StringUtils.hasText(context.getSortParameterName())) {
						builder.addStatement("$T $L = $L.rewrite($L, $L)", String.class, queryStringNameToUse, queryRewriterName,
								queryStringName, context.getSortParameterName());
					} else {
						builder.addStatement("$T $L = $L.rewrite($L, $T.unsorted())", String.class, queryStringNameToUse,
								queryRewriterName, queryStringName, Sort.class);
					}
				}

				if (StringUtils.hasText(sqlResultSetMapping)) {

					builder.addStatement("$T $L = this.$L.createNativeQuery($L, $S)", Query.class, queryVariableName,
							context.fieldNameOf(EntityManager.class), queryStringNameToUse, sqlResultSetMapping);

					return builder.build();
				}

				if (query.isNative()) {

					if (queryReturnType != null) {

						builder.addStatement("$T $L = this.$L.createNativeQuery($L, $T.class)", Query.class, queryVariableName,
								context.fieldNameOf(EntityManager.class), queryStringNameToUse, queryReturnType);
					} else {
						builder.addStatement("$T $L = this.$L.createNativeQuery($L)", Query.class, queryVariableName,
								context.fieldNameOf(EntityManager.class), queryStringNameToUse);
					}

					return builder.build();
				}

				if (sq.hasConstructorExpressionOrDefaultProjection() && !count && methodReturn.isInterfaceProjection()) {
					builder.addStatement("$T $L = this.$L.createQuery($L)", Query.class, queryVariableName,
							context.fieldNameOf(EntityManager.class), queryStringNameToUse);
				} else {

					String createQueryMethod = query.isNative() ? "createNativeQuery" : "createQuery";

					if (!sq.hasConstructorExpressionOrDefaultProjection() && !count && methodReturn.isInterfaceProjection()) {
						builder.addStatement("$T $L = this.$L.$L($L, $T.class)", Query.class, queryVariableName,
								context.fieldNameOf(EntityManager.class), createQueryMethod, queryStringNameToUse, Tuple.class);
					} else {
						builder.addStatement("$T $L = this.$L.$L($L)", Query.class, queryVariableName,
								context.fieldNameOf(EntityManager.class), createQueryMethod, queryStringNameToUse);
					}
				}

				return builder.build();
			}

			if (query instanceof NamedAotQuery nq) {

				if (!count && !nq.hasConstructorExpressionOrDefaultProjection() && methodReturn.isInterfaceProjection()) {
					queryReturnType = Tuple.class;
				}

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
			return identifier.hasName() ? identifier.getName() : Integer.valueOf(identifier.getPosition());
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

				String expressionString = expr.expression().getExpressionString();
				// re-wrap expression
				if (!expressionString.startsWith("$")) {
					expressionString = "#{" + expressionString + "}";
				}

				builder.add("evaluateExpression($L, $S$L)", context.getExpressionMarker().enclosingMethod(), expressionString,
						parameterNames);

				return builder.build();
			}

			throw new UnsupportedOperationException("Not supported yet for: " + origin);
		}

		private CodeBlock applyEntityGraph(AotEntityGraph entityGraph, String queryVariableName) {

			CodeBlock.Builder builder = CodeBlock.builder();

			if (StringUtils.hasText(entityGraph.name())) {

				builder.addStatement("$T<?> $L = $L.getEntityGraph($S)", jakarta.persistence.EntityGraph.class,
						context.localVariable("entityGraph"),
						context.fieldNameOf(EntityManager.class), entityGraph.name());
			} else {

				builder.addStatement("$T<$T> $L = $L.createEntityGraph($T.class)",
						jakarta.persistence.EntityGraph.class, context.getDomainType(),
						context.localVariable("entityGraph"),
						context.fieldNameOf(EntityManager.class), context.getDomainType());

				for (String attributePath : entityGraph.attributePaths()) {

					String[] pathComponents = StringUtils.delimitedListToStringArray(attributePath, ".");

					StringBuilder chain = new StringBuilder(context.localVariable("entityGraph"));
					for (int i = 0; i < pathComponents.length; i++) {

						if (i < pathComponents.length - 1) {
							chain.append(".addSubgraph($S)");
						} else {
							chain.append(".addAttributeNodes($S)");
						}
					}

					builder.addStatement(chain.toString(), (Object[]) pathComponents);
				}

				builder.addStatement("$L.setHint($S, $L)", queryVariableName, entityGraph.type().getKey(),
						context.localVariable("entityGraph"));
			}

			return builder.build();
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
		private final String queryVariableName;
		private @Nullable AotQuery aotQuery;
		private @Nullable String pageable;
		private MergedAnnotation<Modifying> modifying = MergedAnnotation.missing();

		private QueryExecutionBlockBuilder(AotQueryMethodGenerationContext context, JpaQueryMethod queryMethod) {

			this.context = context;
			this.queryMethod = queryMethod;
			this.queryVariableName = context.localVariable("query");
			this.pageable = context.getPageableParameterName() != null ? context.localVariable("pageable") : null;
		}

		public QueryExecutionBlockBuilder query(AotQuery aotQuery) {

			this.aotQuery = aotQuery;
			return this;
		}

		public QueryExecutionBlockBuilder query(String pageable) {

			this.pageable = pageable;
			return this;
		}

		public QueryExecutionBlockBuilder modifying(MergedAnnotation<Modifying> modifying) {

			this.modifying = modifying;
			return this;
		}

		public CodeBlock build() {

			Builder builder = CodeBlock.builder();
			MethodReturn methodReturn = context.getMethodReturn();
			boolean isProjecting = methodReturn.isProjecting()
					|| !ObjectUtils.nullSafeEquals(context.getDomainType(), methodReturn.getActualReturnClass())
					|| StringUtils.hasText(context.getDynamicProjectionParameterName());
			TypeName typeToRead = isProjecting ? methodReturn.getActualTypeName()
					: TypeName.get(context.getDomainType());
			builder.add("\n");

			if (modifying.isPresent()) {

				if (modifying.getBoolean("flushAutomatically")) {
					builder.addStatement("this.$L.flush()", context.fieldNameOf(EntityManager.class));
				}

				Class<?> returnType = methodReturn.toClass();

				if (returnsModifying(returnType)) {
					builder.addStatement("int $L = $L.executeUpdate()", context.localVariable("result"), queryVariableName);
				} else {
					builder.addStatement("$L.executeUpdate()", queryVariableName);
				}

				if (modifying.getBoolean("clearAutomatically")) {
					builder.addStatement("this.$L.clear()", context.fieldNameOf(EntityManager.class));
				}

				if (returnType == int.class || returnType == long.class || returnType == Integer.class) {
					builder.addStatement("return $L", context.localVariable("result"));
				}

				if (returnType == Long.class) {
					builder.addStatement("return (long) $L", context.localVariable("result"));
				}

				return builder.build();
			}

			if (aotQuery != null && aotQuery.isDelete()) {

				builder.addStatement("$T $L = $L.getResultList()", List.class,
						context.localVariable("resultList"), queryVariableName);

				boolean returnCount = ClassUtils.isAssignable(Number.class, methodReturn.toClass());
				boolean simpleBatch = returnCount || methodReturn.isVoid();
				boolean collectionQuery = queryMethod.isCollectionQuery();

				if (!simpleBatch && !collectionQuery) {

					builder.beginControlFlow("if ($L.size() > 1)", context.localVariable("resultList"));
					builder.addStatement("throw new $1T($2S + $3L.size(), 1, $3L.size())",
							IncorrectResultSizeDataAccessException.class,
							"Delete query returned more than one element: expected 1, actual ", context.localVariable("resultList"));
					builder.endControlFlow();
				}

				builder.addStatement("$L.forEach($L::remove)", context.localVariable("resultList"),
						context.fieldNameOf(EntityManager.class));

				if (collectionQuery) {

					if (isStreamable(methodReturn)) {
						builder.addStatement("return ($1T) $1T.of($2L)", Streamable.class, context.localVariable("resultList"));
					} else if (isStreamableWrapper(methodReturn) && canConvert(Streamable.class, methodReturn)) {

						builder.addStatement(
								"return ($1T) $2T.getSharedInstance().convert($3T.of($4L), $5T.valueOf($3T.class), $5T.valueOf($1T.class))",
								methodReturn.toClass(), DefaultConversionService.class, Streamable.class,
								context.localVariable("resultList"), TypeDescriptor.class);
					} else {
						builder.addStatement("return ($T) $L", List.class, context.localVariable("resultList"));
					}
				} else if (returnCount) {
					builder.addStatement("return $T.valueOf($L.size())",
							ClassUtils.resolvePrimitiveIfNecessary(methodReturn.getActualReturnClass()),
								context.localVariable("resultList"));
					} else {

						builder.addStatement(LordOfTheStrings.returning(methodReturn.toClass())
								.optional("($1T) ($2L.isEmpty() ? null : $2L.iterator().next())", typeToRead,
										context.localVariable("resultList")) //
								.build());
					}
			} else if (aotQuery != null && aotQuery.isExists()) {
				builder.addStatement("return !$L.getResultList().isEmpty()", queryVariableName);
			} else if (aotQuery != null) {

				if (isProjecting) {

					TypeName returnType = TypeNames.typeNameOrWrapper(methodReturn.getActualType());
					CodeBlock convertTo;
					if (StringUtils.hasText(context.getDynamicProjectionParameterName())) {
						convertTo = CodeBlock.of("$L", context.getDynamicProjectionParameterName());
					} else {

						if (methodReturn.isArray() && methodReturn.getActualType().toClass().equals(byte.class)) {
							returnType = TypeName.get(byte[].class);
							convertTo = CodeBlock.of("$T.class", returnType);
						} else {
							convertTo = CodeBlock.of("$T.class", TypeNames.classNameOrWrapper(methodReturn.getActualType()));
						}
					}

					if (queryMethod.isCollectionQuery()) {
						builder.addStatement("return ($T) convertMany($L.getResultList(), $L, $L)", methodReturn.getTypeName(),
								queryVariableName, aotQuery.isNative(), convertTo);
					} else if (queryMethod.isStreamQuery()) {
						builder.addStatement("return ($T) convertMany($L.getResultStream(), $L, $L)", methodReturn.getTypeName(),
								queryVariableName, aotQuery.isNative(), convertTo);
					} else if (queryMethod.isPageQuery()) {
						builder.addStatement(
								"return $T.getPage(($T<$T>) convertMany($L.getResultList(), $L, $L), $L, $L)",
								PageableExecutionUtils.class, List.class, TypeNames.typeNameOrWrapper(methodReturn.getActualType()),
								queryVariableName, aotQuery.isNative(), convertTo, pageable, context.localVariable("countAll"));
					} else if (queryMethod.isSliceQuery()) {
						builder.addStatement("$T<$T> $L = ($T<$T>) convertMany($L.getResultList(), $L, $L)", List.class,
								TypeNames.typeNameOrWrapper(methodReturn.getActualType()), context.localVariable("resultList"),
								List.class, typeToRead, queryVariableName,
								aotQuery.isNative(),
								convertTo);
						builder.addStatement("boolean $L = $L.isPaged() && $L.size() > $L.getPageSize()",
								context.localVariable("hasNext"), pageable, context.localVariable("resultList"), pageable);
						builder.addStatement(
								"return new $T<>($L ? $L.subList(0, $L.getPageSize()) : $L, $L, $L)", SliceImpl.class,
								context.localVariable("hasNext"), context.localVariable("resultList"),
								pageable, context.localVariable("resultList"), pageable, context.localVariable("hasNext"));
					} else {

						builder.addStatement(LordOfTheStrings.returning(methodReturn.toClass())
								.optional("($T) convertOne($L.getSingleResultOrNull(), $L, $L)", returnType, queryVariableName,
										aotQuery.isNative(), convertTo) //
								.build());
					}

				} else {

					if (queryMethod.isCollectionQuery()) {
						if (isStreamable(methodReturn)) {
							builder.addStatement("return ($T) $T.of($L.getResultList())", methodReturn.getTypeName(),
									Streamable.class, queryVariableName);
						} else if (isStreamableWrapper(methodReturn) && canConvert(Streamable.class, methodReturn)) {
							builder.addStatement(
									"return ($1T) $2T.getSharedInstance().convert($3T.of($4L.getResultList()), $5T.valueOf($3T.class), $5T.valueOf($1T.class))",
									methodReturn.toClass(), DefaultConversionService.class, Streamable.class, queryVariableName,
									TypeDescriptor.class);
						} else {
							builder.addStatement("return ($T) $L.getResultList()", methodReturn.getTypeName(), queryVariableName);
						}
					} else if (queryMethod.isStreamQuery()) {
						builder.addStatement("return ($T) $L.getResultStream()", methodReturn.getTypeName(), queryVariableName);
					} else if (queryMethod.isPageQuery()) {
						builder.addStatement("return $T.getPage(($T<$T>) $L.getResultList(), $L, $L)",
								PageableExecutionUtils.class, List.class, typeToRead, queryVariableName,
								pageable, context.localVariable("countAll"));
					} else if (queryMethod.isSliceQuery()) {
						builder.addStatement("$T<$T> $L = $L.getResultList()", List.class, typeToRead,
								context.localVariable("resultList"), queryVariableName);
						builder.addStatement("boolean $L = $L.isPaged() && $L.size() > $L.getPageSize()",
								context.localVariable("hasNext"), pageable, context.localVariable("resultList"), pageable);
						builder.addStatement(
								"return new $T<>($L ? $L.subList(0, $L.getPageSize()) : $L, $L, $L)", SliceImpl.class,
								context.localVariable("hasNext"), context.localVariable("resultList"),
								pageable, context.localVariable("resultList"), pageable, context.localVariable("hasNext"));
					} else {

						builder.addStatement(LordOfTheStrings.returning(methodReturn.toClass())
								.optional("($T) convertOne($L.getSingleResultOrNull(), $L, $T.class)",
										TypeNames.typeNameOrWrapper(methodReturn.getActualType()), queryVariableName, aotQuery.isNative(),
										TypeNames.classNameOrWrapper(methodReturn.getActualType())) //
								.build());
					}
				}
			}

			return builder.build();
		}

		private boolean canConvert(Class<?> from, MethodReturn methodReturn) {
			return DefaultConversionService.getSharedInstance().canConvert(from, methodReturn.toClass());
		}

		private static boolean isStreamable(MethodReturn methodReturn) {
			return methodReturn.toClass().equals(Streamable.class);
		}

		private static boolean isStreamableWrapper(MethodReturn methodReturn) {
			return !isStreamable(methodReturn) && Streamable.class.isAssignableFrom(methodReturn.toClass());
		}

		public static boolean returnsModifying(Class<?> returnType) {

			return returnType == int.class || returnType == long.class || returnType == Integer.class
					|| returnType == Long.class;
		}

	}


}
