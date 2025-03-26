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

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
class JpaCodeBlocks {

	private static final Pattern PARAMETER_BINDING_PATTERN = Pattern.compile("\\?(\\d+)");

	public static QueryBlockBuilder queryBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryBlockBuilder(context);
	}

	static QueryExecutionBlockBuilder executionBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryExecutionBlockBuilder(context);
	}

	/**
	 * Builder for the actual query code block.
	 */
	static class QueryBlockBuilder {

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName = "query";
		private AotQueries queries;
		private MergedAnnotation<QueryHints> queryHints = MergedAnnotation.missing();

		private QueryBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
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

		/**
		 * Build the query block.
		 *
		 * @return
		 */
		public CodeBlock build() {

			boolean isProjecting = context.getActualReturnType() != null
					&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
							context.getActualReturnType());
			Object actualReturnType = isProjecting ? context.getActualReturnType()
					: context.getRepositoryInformation().getDomainType();

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");
			String queryStringNameVariableName = "%sString".formatted(queryVariableName);

			StringAotQuery query = (StringAotQuery) queries.result();
			builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, query.getQueryString());

			String countQueryStringNameVariableName = null;
			String countQuyerVariableName = null;

			if (context.returnsPage()) {

				countQueryStringNameVariableName = "count%sString".formatted(StringUtils.capitalize(queryVariableName));
				countQuyerVariableName = "count%s".formatted(StringUtils.capitalize(queryVariableName));

				StringAotQuery countQuery = (StringAotQuery) queries.count();
				builder.addStatement("$T $L = $S", String.class, countQueryStringNameVariableName, countQuery.getQueryString());
			}

			// sorting
			// TODO: refactor into sort builder

			String sortParameterName = context.getSortParameterName();
			if (sortParameterName == null && context.getPageableParameterName() != null) {
				sortParameterName = "%s.getSort()".formatted(context.getPageableParameterName());
			}

			if (StringUtils.hasText(sortParameterName)) {
				builder.add(applySorting(sortParameterName, queryStringNameVariableName, actualReturnType));
			}

			builder.add(createQuery(queryVariableName, queryStringNameVariableName, queries.result(), queryHints));

			builder.add(applyLimits());

			if (StringUtils.hasText(countQueryStringNameVariableName)) {

				builder.beginControlFlow("$T $L = () ->", LongSupplier.class, "countAll");

				boolean queryHints = this.queryHints.isPresent() && this.queryHints.getBoolean("forCounting");

				builder.add(createQuery(countQuyerVariableName, countQueryStringNameVariableName, queries.count(),
						queryHints ? this.queryHints : MergedAnnotation.missing()));
				builder.addStatement("return ($T) $L.getSingleResult()", Long.class, countQuyerVariableName);

				// end control flow does not work well with lambdas
				builder.unindent();
				builder.add("};\n");
			}

			return builder.build();
		}

		private CodeBlock applySorting(String sort, String queryString, Object actualReturnType) {

			Builder builder = CodeBlock.builder();
			builder.beginControlFlow("if ($L.isSorted())", sort);

			if (queries.isNative()) {
				builder.addStatement("$T declaredQuery = $T.nativeQuery($L)", DeclaredQuery.class, DeclaredQuery.class,
						queryString);
			} else {
				builder.addStatement("$T declaredQuery = $T.jpqlQuery($L)", DeclaredQuery.class, DeclaredQuery.class,
						queryString);
			}

			builder.addStatement("$L = rewriteQuery(declaredQuery, $L, $T.class)", queryString, sort, actualReturnType);

			builder.endControlFlow();

			return builder.build();
		}

		private CodeBlock applyLimits() {

			Builder builder = CodeBlock.builder();

			if (context.isExistsMethod()) {
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
				if (context.returnsSlice() && !context.returnsPage()) {
					builder.addStatement("$L.setMaxResults($L.getPageSize() + 1)", queryVariableName, pageable);
				} else {
					builder.addStatement("$L.setMaxResults($L.getPageSize())", queryVariableName, pageable);
				}
				builder.endControlFlow();
			}

			return builder.build();
		}

		private CodeBlock createQuery(String queryVariableName, String queryStringNameVariableName, AotQuery query,
				MergedAnnotation<QueryHints> queryHints) {

			Builder builder = CodeBlock.builder();

			builder.addStatement("$T $L = this.$L.$L($L)", Query.class, queryVariableName,
					context.fieldNameOf(EntityManager.class), query.isNative() ? "createNativeQuery" : "createQuery",
					queryStringNameVariableName);

			if (queryHints.isPresent()) {
				builder.add(applyHints(queryVariableName, queryHints));
				builder.add("\n");
			}

			for (ParameterBinding binding : query.getParameterBindings()) {

				Object prepare = binding.prepare("s");

				if (prepare instanceof String prepared && !prepared.equals("s")) {
					String format = prepared.replaceAll("%", "%%").replace("s", "%s");
					if (binding.getIdentifier().hasPosition()) {
						builder.addStatement("$L.setParameter($L, $S.formatted($L))", queryVariableName,
								binding.getIdentifier().getPosition(), format,
								context.getParameterNameOfPosition(binding.getIdentifier().getPosition() - 1));
					} else {
						builder.addStatement("$L.setParameter($S, $S.formatted($L))", queryVariableName,
								binding.getIdentifier().getName(), format, binding.getIdentifier().getName());
					}
				} else {
					if (binding.getIdentifier().hasPosition()) {
						builder.addStatement("$L.setParameter($L, $L)", queryVariableName, binding.getIdentifier().getPosition(),
								context.getParameterNameOfPosition(binding.getIdentifier().getPosition() - 1));
					} else {
						builder.addStatement("$L.setParameter($S, $L)", queryVariableName, binding.getIdentifier().getName(),
								binding.getIdentifier().getName());
					}
				}
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

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName = "query";

		private QueryExecutionBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		public QueryExecutionBlockBuilder referencing(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		public CodeBlock build() {

			Builder builder = CodeBlock.builder();

			boolean isProjecting = context.getActualReturnType() != null
					&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
							context.getActualReturnType());
			Object actualReturnType = isProjecting ? context.getActualReturnType()
					: context.getRepositoryInformation().getDomainType();
			builder.add("\n");

			if (context.isDeleteMethod()) {

				builder.addStatement("$T<$T> resultList = $L.getResultList()", List.class, actualReturnType, queryVariableName);
				builder.addStatement("resultList.forEach($L::remove)", context.fieldNameOf(EntityManager.class));
				if (context.returnsSingleValue()) {
					if (ClassUtils.isAssignable(Number.class, context.getMethod().getReturnType())) {
						builder.addStatement("return $T.valueOf(resultList.size())", context.getMethod().getReturnType());
					} else {
						builder.addStatement("return resultList.isEmpty() ? null : resultList.iterator().next()");
					}
				} else {
					builder.addStatement("return resultList");
				}
			} else if (context.isExistsMethod()) {
				builder.addStatement("return !$L.getResultList().isEmpty()", queryVariableName);
			} else {

				if (context.returnsSingleValue()) {
					if (context.returnsOptionalValue()) {
						builder.addStatement("return $T.ofNullable(($T) $L.getSingleResultOrNull())", Optional.class,
								actualReturnType, queryVariableName);
					} else {
						builder.addStatement("return ($T) $L.getSingleResultOrNull()", context.getReturnType(), queryVariableName);
					}
				} else if (context.returnsPage()) {
					builder.addStatement("return $T.getPage(($T<$T>) $L.getResultList(), $L, countAll)",
							PageableExecutionUtils.class, List.class, actualReturnType, queryVariableName,
							context.getPageableParameterName());
				} else if (context.returnsSlice()) {
					builder.addStatement("$T<$T> resultList = $L.getResultList()", List.class, actualReturnType,
							queryVariableName);
					builder.addStatement("boolean hasNext = $L.isPaged() && resultList.size() > $L.getPageSize()",
							context.getPageableParameterName(), context.getPageableParameterName());
					builder.addStatement(
							"return new $T<>(hasNext ? resultList.subList(0, $L.getPageSize()) : resultList, $L, hasNext)",
							SliceImpl.class, context.getPageableParameterName(), context.getPageableParameterName());
				} else {
					builder.addStatement("return ($T) query.getResultList()", context.getReturnType());
				}
			}

			return builder.build();

		}

	}


}
