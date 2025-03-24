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

import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.jpa.projection.CollectionAwareProjectionFactory;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaCountQueryCreator;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryCreator;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.aot.generate.AotRepositoryConstructorBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryImplementationMetadata;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryMethodGenerationContext;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class JpaRepositoryContributor extends RepositoryContributor {

	private final CollectionAwareProjectionFactory projectionFactory = new CollectionAwareProjectionFactory();
	private final AotQueryCreator queryCreator;
	private final AotMetaModel metaModel;

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext) {
		super(repositoryContext);

		this.metaModel = new AotMetaModel(repositoryContext.getResolvedTypes());
		this.queryCreator = new AotQueryCreator(metaModel);
	}

	@Override
	protected void customizeFile(RepositoryInformation information, AotRepositoryImplementationMetadata metadata,
			TypeSpec.Builder builder) {
		builder.superclass(TypeName.get(AotRepositoryFragmentSupport.class));
	}

	@Override
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

		constructorBuilder.addParameter("entityManager", EntityManager.class);
		constructorBuilder.addParameter("context", RepositoryFactoryBeanSupport.FragmentCreationContext.class);

		// TODO: Pick up the configured QueryEnhancerSelector
		constructorBuilder.customize((repositoryInformation, builder) -> {
			builder.addStatement("super($T.DEFAULT_SELECTOR, context)", QueryEnhancerSelector.class);
		});
	}

	@Override
	protected AotRepositoryMethodBuilder contributeRepositoryMethod(
			AotRepositoryMethodGenerationContext generationContext) {

		QueryEnhancerSelector selector = QueryEnhancerSelector.DEFAULT_SELECTOR;

		// no stored procedures for now.
		if (AnnotatedElementUtils.findMergedAnnotation(generationContext.getMethod(), Procedure.class) != null) {
			return null;
		}

		Query queryAnnotation = AnnotatedElementUtils.findMergedAnnotation(generationContext.getMethod(), Query.class);
		if (queryAnnotation != null) {
			if (StringUtils.hasText(queryAnnotation.value())
					&& Pattern.compile("[\\?:][#$]\\{.*\\}").matcher(queryAnnotation.value()).find()) {
				return null;
			}
		}

		// TODO: Named query via EntityManager, NamedQuery via properties, also for count queries.

		return new AotRepositoryMethodBuilder(generationContext).customize((context, body) -> {

			MergedAnnotations annotations = MergedAnnotations.from(context.getMethod());

			MergedAnnotation<Query> query = annotations.get(Query.class);
			MergedAnnotation<NativeQuery> nativeQuery = annotations.get(NativeQuery.class);
			MergedAnnotation<QueryHints> queryHints = annotations.get(QueryHints.class);

			body.addCode(context.codeBlocks().logDebug("invoking [%s]".formatted(context.getMethod().getName())));

			AotQueries aotQueries;
			if (query.isPresent() && StringUtils.hasText(query.getString("value"))) {
				aotQueries = buildStringQuery(selector, query);
			} else {
				aotQueries = buildPartTreeQuery(context, query);
			}

			body.addCode(JpaCodeBlocks.queryBlockBuilder(context).filter(aotQueries).build());
			body.addCode(JpaCodeBlocks.queryExecutionBlockBuilder(context).build());
		});
	}

	private AotQueries buildStringQuery(QueryEnhancerSelector selector, MergedAnnotation<Query> query) {

		Function<String, StringAotQuery> queryFunction = query.getBoolean("nativeQuery") ? StringAotQuery::nativeQuery
				: StringAotQuery::jpqlQuery;

		StringAotQuery aotStringQuery = queryFunction.apply(query.getString("value"));
		String countQuery = query.getString("countQuery");

		if (StringUtils.hasText(countQuery)) {
			return AotQueries.from(aotStringQuery, queryFunction.apply(countQuery));
		}

		String countProjection = query.getString("countProjection");
		return AotQueries.from(aotStringQuery, countProjection, selector);
	}

	private AotQueries buildPartTreeQuery(AotRepositoryMethodGenerationContext context, MergedAnnotation<Query> query) {

		PartTree partTree = new PartTree(context.getMethod().getName(), context.getRepositoryInformation().getDomainType());
		// TODO make configurable
		JpqlQueryTemplates templates = JpqlQueryTemplates.UPPER;

		boolean isProjecting = context.getActualReturnType() != null
				&& !ObjectUtils.nullSafeEquals(TypeName.get(context.getRepositoryInformation().getDomainType()),
						context.getActualReturnType());

		Class<?> actualReturnType;
		try {
			actualReturnType = isProjecting
					? ClassUtils.forName(context.getActualReturnType().toString(), context.getClass().getClassLoader())
					: context.getRepositoryInformation().getDomainType();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		ReturnedType returnedType = ReturnedType.of(actualReturnType, context.getRepositoryInformation().getDomainType(),
				projectionFactory);

		ParametersSource parametersSource = ParametersSource.of(context.getRepositoryInformation(), context.getMethod());
		JpaParameters parameters = new JpaParameters(parametersSource);

		AotQuery partTreeQuery = createQuery(partTree, returnedType, parameters, templates);

		if (query.isPresent() && StringUtils.hasText(query.getString("countQuery"))) {
			return AotQueries.from(partTreeQuery, StringAotQuery.jpqlQuery(query.getString("countQuery")));
		}

		AotQuery partTreeCountQuery = createCountQuery(partTree, returnedType, parameters, templates);
		return AotQueries.from(partTreeQuery, partTreeCountQuery);
	}

	private AotQuery createQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				templates);
		JpaQueryCreator queryCreator = new JpaQueryCreator(partTree, returnedType, metadataProvider, templates, metaModel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(),
				partTree.getResultLimit());
	}

	private AotQuery createCountQuery(PartTree partTree, ReturnedType returnedType, JpaParameters parameters,
			JpqlQueryTemplates templates) {

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters, EscapeCharacter.DEFAULT,
				templates);
		JpaQueryCreator queryCreator = new JpaCountQueryCreator(partTree, returnedType, metadataProvider, templates,
				metaModel);

		return StringAotQuery.jpqlQuery(queryCreator.createQuery(), metadataProvider.getBindings(), null);
	}

}
