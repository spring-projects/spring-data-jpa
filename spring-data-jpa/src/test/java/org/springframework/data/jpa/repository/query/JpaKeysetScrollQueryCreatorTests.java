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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Unit tests for {@link JpaKeysetScrollQueryCreator}.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class JpaKeysetScrollQueryCreatorTests {

	@PersistenceContext EntityManager entityManager;

	@Test // GH-3588
	void shouldCreateContinuationQuery() throws Exception {

		Map<String, Object> keys = Map.of("id", "10", "firstname", "John", "emailAddress", "john@example.com");
		KeysetScrollPosition position = ScrollPosition.of(keys, ScrollPosition.Direction.BACKWARD);

		Method method = MyRepo.class.getMethod("findTop3ByFirstnameStartingWithOrderByFirstnameAscEmailAddressAsc",
				String.class, ScrollPosition.class);

		PersistenceProvider provider = PersistenceProvider.fromEntityManager(entityManager);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, AbstractRepositoryMetadata.getMetadata(MyRepo.class),
				new SpelAwareProxyProjectionFactory(), provider);

		PartTree tree = new PartTree("findTop3ByFirstnameStartingWithOrderByFirstnameAscEmailAddressAsc", User.class);
		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(
				queryMethod.getParameters(), EscapeCharacter.DEFAULT, JpqlQueryTemplates.UPPER);

		JpaMetamodelEntityInformation<User, User> entityInformation = new JpaMetamodelEntityInformation<>(User.class,
				entityManager.getMetamodel(), entityManager.getEntityManagerFactory().getPersistenceUnitUtil());
		JpaKeysetScrollQueryCreator creator = new JpaKeysetScrollQueryCreator(tree,
				queryMethod.getResultProcessor().getReturnedType(), metadataProvider, JpqlQueryTemplates.UPPER,
				entityInformation, position, entityManager);

		String query = creator.createQuery();

		assertThat(query).containsIgnoringWhitespaces("""
				SELECT u FROM org.springframework.data.jpa.domain.sample.User u WHERE (u.firstname LIKE ?1 ESCAPE '\\')
				AND (u.firstname < ?2
				OR u.firstname = ?3 AND u.emailAddress < ?4
				OR u.firstname = ?5 AND u.emailAddress = ?6 AND u.id < ?7)
				ORDER BY u.firstname desc, u.emailAddress desc, u.id desc
				""");
	}

	interface MyRepo extends Repository<User, String> {

		Window<User> findTop3ByFirstnameStartingWithOrderByFirstnameAscEmailAddressAsc(String firstname,
				ScrollPosition position);

	}

}
