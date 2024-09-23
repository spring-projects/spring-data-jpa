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

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.ParameterBinding;
import org.springframework.data.jpa.repository.query.QueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryEnhancer.QueryRewriteInformation;
import org.springframework.data.jpa.repository.query.QueryEnhancerFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.CodeBlock.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @since 2025/01
 */
public class JpaCodeBlocks {

	private static final Pattern PARAMETER_BINDING_PATTERN = Pattern.compile("\\?(\\d+)");

	static QueryBlockBuilder queryBlockBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryBlockBuilder(context);
	}

	static QueryExecutionBlockBuilder queryExecutionBlockBuilder(AotRepositoryMethodGenerationContext context) {
		return new QueryExecutionBlockBuilder(context);
	}

	static class QueryExecutionBlockBuilder {

		AotRepositoryMethodGenerationContext context;
		private String queryVariableName;

		public QueryExecutionBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryExecutionBlockBuilder referencing(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		CodeBlock build() {

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

	static class QueryBlockBuilder {

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName;
		private AotStringQuery query;

		public QueryBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryBlockBuilder usingQueryVariableName(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		QueryBlockBuilder filter(String queryString) {
			return filter(AotStringQuery.of(queryString));
		}

		QueryBlockBuilder filter(AotStringQuery query) {
			this.query = query;
			return this;
		}

		CodeBlock build() {

			boolean isProjecting = context.getActualReturnType() != null
				&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
				context.getActualReturnType());
			Object actualReturnType = isProjecting ? context.getActualReturnType()
				: context.getRepositoryInformation().getDomainType();

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");
			String queryStringNameVariableName = "%sString".formatted(queryVariableName);
			builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, query.getQueryString());

			String countQueryStringNameVariableName = null;
			String countQuyerVariableName = null;
			if (context.returnsPage()) {
				countQueryStringNameVariableName = "count%sString".formatted(StringUtils.capitalize(queryVariableName));
				countQuyerVariableName = "count%s".formatted(StringUtils.capitalize(queryVariableName));
				String projection = context.annotationValue(org.springframework.data.jpa.repository.Query.class,
						"countProjection");
				builder.addStatement("$T $L = $S", String.class, countQueryStringNameVariableName,
						query.getCountQuery(projection));
			}

			// sorting
			// TODO: refactor into sort builder
			{
				String sortParameterName = context.getSortParameterName();
				if (sortParameterName == null && context.getPageableParameterName() != null) {
					sortParameterName = "%s.getSort()".formatted(context.getPageableParameterName());
				}

				if (StringUtils.hasText(sortParameterName)) {
					builder.beginControlFlow("if($L.isSorted())", sortParameterName);

					if(query.isNativeQuery()) {
						builder.addStatement("$T declaredQuery = $T.nativeQuery($L)", DeclaredQuery.class, DeclaredQuery.class,
							queryStringNameVariableName);
					} else {
						builder.addStatement("$T declaredQuery = $T.jpqlQuery($L)", DeclaredQuery.class, DeclaredQuery.class,
							queryStringNameVariableName);
					}

					String enhancerVarName = "%sEnhancer".formatted(queryStringNameVariableName);
					builder.addStatement("$T $L = $T.forQuery(declaredQuery).create(declaredQuery)", QueryEnhancer.class, enhancerVarName, QueryEnhancerFactory.class);

					builder.addStatement("$L = $L.rewrite(new $T() { public $T getSort() { return $L; } public $T getReturnedType() { return $T.of($T.class, $T.class, new $T());}  })", queryStringNameVariableName, enhancerVarName, QueryRewriteInformation.class,
						Sort.class, sortParameterName, ReturnedType.class, ReturnedType.class,
						context.getRepositoryInformation().getDomainType(), actualReturnType, SpelAwareProxyProjectionFactory.class);

					builder.endControlFlow();
				}
			}

			addQueryBlock(builder, queryVariableName, queryStringNameVariableName, query.isNativeQuery());

			if (context.isExistsMethod()) {
				builder.addStatement("$L.setMaxResults(1)", queryVariableName);
			} else {

				{
					String limitParameterName = context.getLimitParameterName();

					if (StringUtils.hasText(limitParameterName)) {
						builder.beginControlFlow("if($L.isLimited())", limitParameterName);
						builder.addStatement("$L.setMaxResults($L.max())", queryVariableName, limitParameterName);
						builder.endControlFlow();
					} else if (query.isLimited()) {
						builder.addStatement("$L.setMaxResults($L)", queryVariableName, query.getLimit().max());
					}
				}

				{
					String pageableParamterName = context.getPageableParameterName();
					if (StringUtils.hasText(pageableParamterName)) {
						builder.beginControlFlow("if($L.isPaged())", pageableParamterName);
						builder.addStatement("$L.setFirstResult(Long.valueOf($L.getOffset()).intValue())", queryVariableName,
								pageableParamterName);
						if (context.returnsSlice() && !context.returnsPage()) {
							builder.addStatement("$L.setMaxResults($L.getPageSize() + 1)", queryVariableName, pageableParamterName);
						} else {
							builder.addStatement("$L.setMaxResults($L.getPageSize())", queryVariableName, pageableParamterName);
						}
						builder.endControlFlow();
					}
				}
			}

			if (StringUtils.hasText(countQueryStringNameVariableName)) {
				builder.beginControlFlow("$T $L = () ->", LongSupplier.class, "countAll");
				addQueryBlock(builder, countQuyerVariableName, countQueryStringNameVariableName, query.isNativeQuery());
				builder.addStatement("return ($T) $L.getSingleResult()", Long.class, countQuyerVariableName);

				// end control flow does not work well with lambdas
				builder.unindent();
				builder.add("};\n");
			}

			return builder.build();
		}

		private void addQueryBlock(Builder builder, String queryVariableName, String queryStringNameVariableName,
				boolean nativeQuery) {

			builder.addStatement("$T $L = this.$L.$L($L)", Query.class, queryVariableName,
					context.fieldNameOf(EntityManager.class), nativeQuery ? "createNativeQuery" : "createQuery",
					queryStringNameVariableName);

			for (ParameterBinding binding : query.parameterBindings()) {

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
		}
	}
}
