/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.proxy.HibernateProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Category;
import org.springframework.data.jpa.domain.sample.OrmXmlEntity;
import org.springframework.data.jpa.domain.sample.Product;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.CategoryRepository;
import org.springframework.data.jpa.repository.sample.ProductRepository;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests for {@link JpaMetamodelMappingContext}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @since 1.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaMetamodelMappingContextIntegrationTests {

	JpaMetamodelMappingContext context;
	@Autowired ProductRepository products;
	@Autowired CategoryRepository categories;
	@Autowired EntityManager em;
	@Autowired PlatformTransactionManager transactionManager;

	@Before
	public void setUp() {
		context = new JpaMetamodelMappingContext(Collections.singleton(em.getMetamodel()));
	}

	@Test
	public void setsUpMappingContextCorrectly() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(User.class);
		assertThat(entity).isNotNull();
	}

	@Test
	public void detectsIdProperty() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(User.class);
		assertThat(entity.getIdProperty()).isNotNull();
	}

	@Test
	public void detectsAssociation() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(User.class);
		assertThat(entity).isNotNull();

		JpaPersistentProperty property = entity.getRequiredPersistentProperty("manager");
		assertThat(property.isAssociation()).isTrue();
	}

	@Test
	public void detectsPropertyIsEntity() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(User.class);
		assertThat(entity).isNotNull();

		JpaPersistentProperty property = entity.getRequiredPersistentProperty("manager");
		assertThat(property.isEntity()).isTrue();

		property = entity.getRequiredPersistentProperty("lastname");
		assertThat(property.isEntity()).isFalse();
	}

	@Test // DATAJPA-608
	public void detectsEntityPropertyForCollections() {

		JpaPersistentEntityImpl<?> entity = context.getRequiredPersistentEntity(User.class);
		assertThat(entity).isNotNull();

		assertThat(entity.getRequiredPersistentProperty("colleagues").isEntity()).isTrue();
	}

	@Test // DATAJPA-630
	public void lookingUpIdentifierOfProxyDoesNotInitializeProxy() {

		TransactionTemplate template = new TransactionTemplate(transactionManager);
		final Category category = template.execute(status -> {

			Product product = products.save(new Product());
			return categories.save(new Category(product));
		});

		template.execute(status -> {

			Category loaded = categories.findById(category.getId()).get();
			Product loadedProduct = loaded.getProduct();

			JpaPersistentEntity<?> entity = context.getRequiredPersistentEntity(Product.class);
			IdentifierAccessor accessor = entity.getIdentifierAccessor(loadedProduct);

			assertThat(accessor.getIdentifier()).isEqualTo(category.getProduct().getId());
			assertThat(loadedProduct).isInstanceOf(HibernateProxy.class);
			assertThat(((HibernateProxy) loadedProduct).getHibernateLazyInitializer().isUninitialized()).isTrue();

			status.setRollbackOnly();

			return null;
		});
	}

	/**
	 * @see DATAJPA-658
	 */
	@Test
	public void shouldDetectIdPropertyForEntityConfiguredViaOrmXmlWithoutAnyAnnotations() {

		JpaPersistentEntity<?> entity = context.getPersistentEntity(OrmXmlEntity.class);

		assertThat(entity.getIdProperty()).isNotNull();
	}

	@Test // DATAJPA-1320
	public void detectsEmbeddableProperty() {

		JpaPersistentEntity<?> persistentEntity = context.getPersistentEntity(User.class);
		JpaPersistentProperty property = persistentEntity.getPersistentProperty("address");

		assertThat(property.isEmbeddable()).isTrue();
	}

	@Test // DATAJPA-1320
	public void traversesEmbeddablesButNoOtherMappingAnnotations() {

		PersistentPropertyPaths<User, JpaPersistentProperty> paths = //
				context.findPersistentPropertyPaths(User.class, __ -> true);

		assertThat(paths.contains("address.city")).isTrue();

		// Exists but is not selected
		assertThat(context.getPersistentPropertyPath("colleagues.firstname", User.class)).isNotNull();
		assertThat(paths.contains("colleagues.firstname")).isFalse();
	}

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(basePackageClasses = CategoryRepository.class, //
			includeFilters = @Filter(value = { CategoryRepository.class, ProductRepository.class },
					type = FilterType.ASSIGNABLE_TYPE))
	static class Config {

	}
}
