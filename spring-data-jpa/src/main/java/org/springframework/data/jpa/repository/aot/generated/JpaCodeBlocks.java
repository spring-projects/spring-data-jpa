/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https	://www.apache.org/licenses/LICENSE-2.0
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

import java.lang.reflect.Parameter;
import java.util.regex.Pattern;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.QueryEnhancerFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.javapoet.CodeBlock;
import org.springframework.util.ClassUtils;
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

	static class QueryBlockBuilder {

		private final AotRepositoryMethodGenerationContext context;
		private String queryVariableName;
		private String queryString;

		public QueryBlockBuilder(AotRepositoryMethodGenerationContext context) {
			this.context = context;
		}

		QueryBlockBuilder usingQueryVariableName(String queryVariableName) {

			this.queryVariableName = queryVariableName;
			return this;
		}

		QueryBlockBuilder filter(String queryString) {
			this.queryString = queryString;
			return this;
		}

		CodeBlock build() {

			CodeBlock.Builder builder = CodeBlock.builder();
			builder.add("\n");
			String queryStringNameVariableName = "%sString".formatted(queryVariableName);
			builder.addStatement("$T $L = $S", String.class, queryStringNameVariableName, queryString);

			// sorting
			{
				String sortParameterName = context.getSortParameterName();
				if(sortParameterName == null && context.getPageableParameterName() != null) {
					sortParameterName = "%s.getSort()".formatted(context.getPageableParameterName());
				}

				if (StringUtils.hasText(sortParameterName)) {
					builder.beginControlFlow("if($L.isSorted())", sortParameterName);

					builder.addStatement("$T declaredQuery = $T.of($L, false)", DeclaredQuery.class, DeclaredQuery.class,
							queryStringNameVariableName);
					builder.addStatement("$L = $T.forQuery(declaredQuery).applySorting($L)", queryStringNameVariableName,
							QueryEnhancerFactory.class, sortParameterName);

					builder.endControlFlow();
				}
			}

			builder.addStatement("$T $L = this.$L.createQuery($L)", Query.class, queryVariableName,
					context.fieldNameOf(EntityManager.class), queryStringNameVariableName);

			int i = 1;
			for (Parameter parameter : context.getMethod().getParameters()) {
				if (ClassUtils.isAssignable(Sort.class, parameter.getType())
						|| ClassUtils.isAssignable(Pageable.class, parameter.getType())) {
					// skip
				} else if (ClassUtils.isAssignable(Limit.class, parameter.getType())) {
					builder.addStatement("$L.setParameter(" + i + ", " + parameter.getName() + ".max())", queryVariableName);
				} else {
					builder.addStatement("$L.setParameter(" + i + ", " + parameter.getName() + ")", queryVariableName);
				}
				i++;
			}

			return builder.build();
		}
	}
}
