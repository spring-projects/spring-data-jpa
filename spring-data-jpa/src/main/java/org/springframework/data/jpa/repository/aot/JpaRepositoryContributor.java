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

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.spi.PersistenceUnitInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.query.JpaEntityMetadata;
import org.springframework.data.jpa.repository.query.JpaParameters;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.repository.query.QueryEnhancerSelector;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.aot.generate.AotRepositoryClassBuilder;
import org.springframework.data.repository.aot.generate.AotRepositoryConstructorBuilder;
import org.springframework.data.repository.aot.generate.MethodContributor;
import org.springframework.data.repository.aot.generate.QueryMetadata;
import org.springframework.data.repository.aot.generate.RepositoryContributor;
import org.springframework.data.repository.config.AotRepositoryContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.query.ParametersSource;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.TypeName;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * JPA-specific {@link RepositoryContributor} contributing an AOT repository fragment using the {@link EntityManager}
 * directly to run queries.
 * <p>
 * The underlying {@link jakarta.persistence.metamodel.Metamodel} requires Hibernate to build metamodel information.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
public class JpaRepositoryContributor extends RepositoryContributor {

	private final AotRepositoryContext context;
	private final PersistenceUnitContext persistenceUnit;
	private final Metamodel metamodel;
	private final PersistenceUnitUtil persistenceUnitUtil;
	private final PersistenceProvider persistenceProvider;
	private final QueriesFactory queriesFactory;
	private final EntityGraphLookup entityGraphLookup;

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext) {
		this(repositoryContext, PersistenceUnitContextFactory.from(repositoryContext).create());
	}

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext, EntityManagerFactory entityManagerFactory) {
		this(repositoryContext, PersistenceUnitContext.just(entityManagerFactory));
	}

	public JpaRepositoryContributor(AotRepositoryContext repositoryContext, PersistenceUnitContext persistenceUnit) {

		super(repositoryContext);

		this.persistenceUnit = persistenceUnit;
		this.metamodel = persistenceUnit.getMetamodel();

		EntityManagerFactory entityManagerFactory = persistenceUnit.getEntityManagerFactory();

		this.persistenceUnitUtil = entityManagerFactory.getPersistenceUnitUtil();
		this.persistenceProvider = PersistenceProvider.fromEntityManagerFactory(entityManagerFactory);
		this.queriesFactory = new QueriesFactory(repositoryContext.getConfigurationSource(), entityManagerFactory,
				repositoryContext.getRequiredClassLoader());
		this.entityGraphLookup = new EntityGraphLookup(entityManagerFactory);
		this.context = repositoryContext;
	}

	@Override
	protected void customizeClass(AotRepositoryClassBuilder classBuilder) {
		classBuilder.customize(builder -> builder.superclass(TypeName.get(AotRepositoryFragmentSupport.class)));
	}

	@Override
	protected void customizeConstructor(AotRepositoryConstructorBuilder constructorBuilder) {

		String entityManagerFactoryRef = getEntityManagerFactoryRef();

		constructorBuilder.addParameter("entityManager", EntityManager.class, customizer -> {

			customizer.bindToField().origin(
					StringUtils.hasText(entityManagerFactoryRef)
							? new RuntimeBeanReference(entityManagerFactoryRef, EntityManager.class)
							: new RuntimeBeanReference(EntityManager.class));
		});

		constructorBuilder.addParameter("context", RepositoryFactoryBeanSupport.FragmentCreationContext.class);

		Optional<Class<QueryEnhancerSelector>> queryEnhancerSelector = getQueryEnhancerSelectorClass();

		constructorBuilder.customize(builder -> {

			if (queryEnhancerSelector.isPresent()) {
				builder.addStatement("super(new T$(), context)", queryEnhancerSelector.get());
			} else {
				builder.addStatement("super($T.DEFAULT_SELECTOR, context)", QueryEnhancerSelector.class);
			}
		});
	}

	private @Nullable String getEntityManagerFactoryRef() {
		return context.getConfigurationSource().getAttribute("entityManagerFactoryRef")
				.filter(it -> !"entityManagerFactory".equals(it)).orElse(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Optional<Class<QueryEnhancerSelector>> getQueryEnhancerSelectorClass() {
		return (Optional) context.getConfigurationSource().getAttribute("queryEnhancerSelector", Class.class)
				.filter(it -> !it.equals(QueryEnhancerSelector.DefaultQueryEnhancerSelector.class));
	}

	@Override
	protected @Nullable MethodContributor<? extends QueryMethod> contributeQueryMethod(Method method) {

		JpaEntityMetadata<?> entityInformation = JpaEntityInformationSupport
				.getEntityInformation(getRepositoryInformation().getDomainType(), metamodel, persistenceUnitUtil);
		AotJpaQueryMethod queryMethod = new AotJpaQueryMethod(method, getRepositoryInformation(), entityInformation,
				getProjectionFactory(), persistenceProvider, JpaParameters::new);

		Optional<Class<QueryEnhancerSelector>> queryEnhancerSelectorClass = getQueryEnhancerSelectorClass();
		QueryEnhancerSelector selector = queryEnhancerSelectorClass.map(BeanUtils::instantiateClass)
				.orElse(QueryEnhancerSelector.DEFAULT_SELECTOR);

		// no stored procedures for now.
		if (queryMethod.isProcedureQuery()) {

			Procedure procedure = AnnotatedElementUtils.findMergedAnnotation(method, Procedure.class);

			MethodContributor.QueryMethodMetadataContributorBuilder<JpaQueryMethod> builder = MethodContributor
					.forQueryMethod(queryMethod);

			if (procedure != null) {

				if (StringUtils.hasText(procedure.name())) {
					return builder.metadataOnly(new NamedStoredProcedureMetadata(procedure.name()));
				}

				if (StringUtils.hasText(procedure.procedureName())) {
					return builder.metadataOnly(new StoredProcedureMetadata(procedure.procedureName()));
				}

				if (StringUtils.hasText(procedure.value())) {
					return builder.metadataOnly(new StoredProcedureMetadata(procedure.value()));
				}
			}

			// TODO: Better fallback.
			return null;
		}

		ReturnedType returnedType = queryMethod.getResultProcessor().getReturnedType();
		JpaParameters parameters = queryMethod.getParameters();

		MergedAnnotation<Query> query = MergedAnnotations.from(method).get(Query.class);

		AotQueries aotQueries = queriesFactory.createQueries(getRepositoryInformation(), returnedType, selector, query,
				queryMethod);

		// no KeysetScrolling for now.
		if (parameters.hasScrollPositionParameter() || queryMethod.isScrollQuery()) {
			return MethodContributor.forQueryMethod(queryMethod)
					.metadataOnly(aotQueries.toMetadata(queryMethod.isPageQuery()));
		}

		// no dynamic projections.
		if (parameters.hasDynamicProjection()) {
			return MethodContributor.forQueryMethod(queryMethod)
					.metadataOnly(aotQueries.toMetadata(queryMethod.isPageQuery()));
		}

		if (queryMethod.isModifyingQuery()) {

			TypeInformation<?> returnType = getRepositoryInformation().getReturnType(method);

			boolean returnsCount = JpaCodeBlocks.QueryExecutionBlockBuilder.returnsModifying(returnType.getType());
			boolean isVoid = ClassUtils.isVoidType(returnType.getType());

			if (!returnsCount && !isVoid) {
				return MethodContributor.forQueryMethod(queryMethod)
						.metadataOnly(aotQueries.toMetadata(queryMethod.isPageQuery()));
			}
		}

		return MethodContributor.forQueryMethod(queryMethod).withMetadata(aotQueries.toMetadata(queryMethod.isPageQuery()))
				.contribute(context -> {

					CodeBlock.Builder body = CodeBlock.builder();

					MergedAnnotation<NativeQuery> nativeQuery = context.getAnnotation(NativeQuery.class);
					MergedAnnotation<QueryHints> queryHints = context.getAnnotation(QueryHints.class);
					MergedAnnotation<EntityGraph> entityGraph = context.getAnnotation(EntityGraph.class);
					MergedAnnotation<Modifying> modifying = context.getAnnotation(Modifying.class);

					AotEntityGraph aotEntityGraph = entityGraphLookup.findEntityGraph(entityGraph, getRepositoryInformation(),
							returnedType, queryMethod);

					body.add(JpaCodeBlocks.queryBuilder(context, queryMethod).filter(aotQueries)
							.queryReturnType(QueriesFactory.getQueryReturnType(aotQueries.result(), returnedType, context))
							.nativeQuery(nativeQuery).queryHints(queryHints).entityGraph(aotEntityGraph)
							.queryRewriter(query.isPresent() ? query.getClass("queryRewriter") : null).build());

					body.add(JpaCodeBlocks.executionBuilder(context, queryMethod).modifying(modifying).query(aotQueries.result())
							.build());

					return body.build();
				});
	}

	public PersistenceUnitContext getPersistenceUnit() {
		return persistenceUnit;
	}

	/**
	 * Factory for deferred {@link PersistenceUnitContext} creation. Factory objects implement equality checks based on
	 * their creation and can be used conveniently as cache keys.
	 */
	public static class PersistenceUnitContextFactory {

		private final Supplier<? extends PersistenceUnitContext> factory;
		private final Object key;

		private PersistenceUnitContextFactory(Supplier<? extends PersistenceUnitContext> factory, Object key) {
			this.factory = Lazy.of(factory);
			this.key = key;
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link AotRepositoryContext} using Jakarta
		 * Persistence-annotated classes.
		 *
		 * @param repositoryContext repository context providing classes.
		 */
		public static PersistenceUnitContextFactory from(AotRepositoryContext repositoryContext) {

			List<String> typeNames = repositoryContext.getResolvedTypes().stream()
					.filter(PersistenceUnitContextFactory::isJakartaAnnotated).map(Class::getName).toList();

			return from(() -> new AotMetamodel(PersistenceManagedTypes.of(typeNames, List.of())), typeNames);
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceUnitInfo}.
		 *
		 * @param persistenceUnitInfo persistence unit info to use.
		 */
		public static PersistenceUnitContextFactory from(PersistenceUnitInfo persistenceUnitInfo) {
			return from(() -> new AotMetamodel(persistenceUnitInfo), persistenceUnitInfo);
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceManagedTypes}.
		 *
		 * @param managedTypes managed types to use.
		 */
		public static PersistenceUnitContextFactory from(PersistenceManagedTypes managedTypes) {
			return from(() -> new AotMetamodel(managedTypes), managedTypes);
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link EntityManagerFactory} and its {@link Metamodel}.
		 *
		 * @param entityManagerFactory the entity manager factory to use.
		 */
		public static PersistenceUnitContextFactory just(EntityManagerFactory entityManagerFactory) {
			return new PersistenceUnitContextFactory(() -> new EntityManagerPersistenceUnitContext(entityManagerFactory),
					entityManagerFactory.getMetamodel());
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link EntityManagerFactory} and its {@link Metamodel}.
		 *
		 * @param metamodel the metamodel to use.
		 * @param entityManagerFactory the entity manager factory to use.
		 */
		public static PersistenceUnitContextFactory just(EntityManagerFactory entityManagerFactory, Metamodel metamodel) {
			return new PersistenceUnitContextFactory(
					() -> new EntityManagerPersistenceUnitContext(entityManagerFactory, metamodel), metamodel);
		}

		private static PersistenceUnitContextFactory from(Supplier<? extends AotMetamodel> metamodel, Object key) {
			return new PersistenceUnitContextFactory(() -> new AotMetamodelContext(metamodel.get()), key);
		}

		private static boolean isJakartaAnnotated(Class<?> cls) {

			return cls.isAnnotationPresent(Entity.class) //
					|| cls.isAnnotationPresent(Embeddable.class) //
					|| cls.isAnnotationPresent(MappedSuperclass.class) //
					|| cls.isAnnotationPresent(Converter.class);
		}

		public PersistenceUnitContext create() {
			return factory.get();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof PersistenceUnitContextFactory that)) {
				return false;
			}
			return ObjectUtils.nullSafeEquals(key, that.key);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(key);
		}

		@Override
		public String toString() {
			return "PersistenceUnitContextFactory{" + key + '}';
		}

	}

	/**
	 * Strategy interface representing a JPA PersistenceUnit providing access to {@link EntityManagerFactory} and
	 * {@link Metamodel} for AOT repository generation.
	 */
	public interface PersistenceUnitContext {

		/**
		 * @return the entity manager factory.
		 */
		EntityManagerFactory getEntityManagerFactory();

		/**
		 * @return metamodel describing managed types used in the persistence unit.
		 */
		Metamodel getMetamodel();

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceUnitInfo}.
		 *
		 * @param persistenceUnitInfo persistence unit info to use.
		 */
		static PersistenceUnitContext from(PersistenceUnitInfo persistenceUnitInfo) {
			return new AotMetamodelContext(new AotMetamodel(persistenceUnitInfo));
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link PersistenceManagedTypes}.
		 *
		 * @param managedTypes managed types to use.
		 */
		static PersistenceUnitContext from(PersistenceManagedTypes managedTypes) {
			return new AotMetamodelContext(new AotMetamodel(managedTypes));
		}

		/**
		 * Create a {@code PersistenceUnitContext} from the given {@link EntityManagerFactory} and its {@link Metamodel}.
		 *
		 * @param entityManagerFactory the entity manager factory to use.
		 */
		static PersistenceUnitContext just(EntityManagerFactory entityManagerFactory) {
			return new EntityManagerPersistenceUnitContext(entityManagerFactory);
		}

	}

	/**
	 * Persistence unit context backed by an {@link EntityManagerFactory}.
	 */
	record EntityManagerPersistenceUnitContext(EntityManagerFactory factory,
			Metamodel metamodel) implements PersistenceUnitContext {

		public EntityManagerPersistenceUnitContext(EntityManagerFactory factory) {
			this(factory, factory.getMetamodel());
		}

		@Override
		public Metamodel getMetamodel() {
			return metamodel();
		}

		@Override
		public EntityManagerFactory getEntityManagerFactory() {
			return factory();
		}

	}

	/**
	 * Persistence unit context backed by an {@link AotMetamodel}.
	 */
	private record AotMetamodelContext(AotMetamodel metamodel) implements PersistenceUnitContext {

		@Override
		public EntityManagerFactory getEntityManagerFactory() {
			return metamodel.getEntityManagerFactory();
		}

		@Override
		public Metamodel getMetamodel() {
			return metamodel;
		}

	}

	record StoredProcedureMetadata(String procedure) implements QueryMetadata {

		@Override
		public Map<String, Object> serialize() {
			return Map.of("procedure", procedure());
		}

	}

	record NamedStoredProcedureMetadata(String procedureName) implements QueryMetadata {

		@Override
		public Map<String, Object> serialize() {
			return Map.of("procedure-name", procedureName());
		}

	}

	/**
	 * AOT extension to {@link JpaQueryMethod} providing a metamodel backed {@link JpaEntityMetadata} object.
	 */
	static class AotJpaQueryMethod extends JpaQueryMethod {

		private final JpaEntityMetadata<?> entityMetadata;

		public AotJpaQueryMethod(Method method, RepositoryMetadata metadata, JpaEntityMetadata<?> entityMetadata,
				ProjectionFactory factory, QueryExtractor extractor,
				Function<ParametersSource, JpaParameters> parametersFunction) {

			super(method, metadata, factory, extractor, parametersFunction);

			this.entityMetadata = entityMetadata;
		}

		@Override
		public JpaEntityMetadata<?> getEntityInformation() {
			return this.entityMetadata;
		}

	}

}
