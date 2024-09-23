/*
 * Copyright 2024 the original author or authors.
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

import java.util.regex.Pattern;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.jpa.projection.CollectionAwareProjectionFactory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.aot.generate.AotRepositoryConstructorBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.javapoet.TypeName;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 */
public class JpaRepsoitoryContributor extends RepositoryContributor {

	AotQueryCreator queryCreator;
	AotMetaModel metaModel;

	public JpaRepsoitoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);

		metaModel = new AotMetaModel(repositoryContext.getResolvedTypes());
		this.queryCreator = new AotQueryCreator(metaModel);
	}

	@Override
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {
		constructorBuilder.addParameter("entityManager", TypeName.get(EntityManager.class));
	}

	@Override
	protected AotRepositoryMethodBuilder contributeRepositoryMethod(
			AotRepositoryMethodGenerationContext generationContext) {

		{
			Query queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(generationContext.getMethod(), Query.class);
			if (queryAnnotation != null) {
				if (StringUtils.hasText(queryAnnotation.value())
						&& Pattern.compile("[\\?:][#$]\\{.*\\}").matcher(queryAnnotation.value()).find()) {
					return null;
				}
			}
		}

		return new AotRepositoryMethodBuilder(generationContext).customize((context, body) -> {

			Query query = AnnotatedElementUtils.findMergedAnnotation(context.getMethod(), Query.class);
			if (query != null && StringUtils.hasText(query.value())) {

				AotStringQuery aotStringQuery = query.nativeQuery() ? AotStringQuery.nativeQuery(query.value())
						: AotStringQuery.of(query.value());
				aotStringQuery.setCountQuery(query.countQuery());
				body.addCode(context.codeBlocks().logDebug("invoking [%s]".formatted(context.getMethod().getName())));

				body.addCode(

						JpaCodeBlocks.queryBlockBuilder(context).usingQueryVariableName("query").filter(aotStringQuery).build());
			} else {

				PartTree partTree = new PartTree(context.getMethod().getName(),
						context.getRepositoryInformation().getDomainType());

				CollectionAwareProjectionFactory projectionFactory = new CollectionAwareProjectionFactory();

				boolean isProjecting = context.getActualReturnType() != null
						&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
								context.getActualReturnType());

				Class<?> actualReturnType = context.getRepositoryInformation().getDomainType();
				try {
					actualReturnType = isProjecting
							? ClassUtils.forName(context.getActualReturnType().toString(), context.getClass().getClassLoader())
							: context.getRepositoryInformation().getDomainType();
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}

				ReturnedType returnedType = ReturnedType.of(actualReturnType,
						context.getRepositoryInformation().getDomainType(), projectionFactory);
				AotStringQuery stringQuery = queryCreator.createQuery(partTree, returnedType, context);

				body.addCode(context.codeBlocks().logDebug("invoking [%s]".formatted(context.getMethod().getName())));
				body.addCode(
						JpaCodeBlocks.queryBlockBuilder(context).usingQueryVariableName("query").filter(stringQuery).build());
			}
			body.addCode(JpaCodeBlocks.queryExecutionBlockBuilder(context).referencing("query").build());
		});
	}
}
