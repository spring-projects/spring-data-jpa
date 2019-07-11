/*
 * Copyright 2011-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Collections;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AbstractJpaEntityInformation}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaEntityInformationSupportUnitTests {

	@Mock EntityManager em;
	@Mock Metamodel metaModel;

	@Test
	public void usesSimpleClassNameIfNoEntityNameGiven() throws Exception {

		JpaEntityInformation<User, Long> information = new DummyJpaEntityInformation<User, Long>(User.class);
		assertThat(information.getEntityName()).isEqualTo("User");

		JpaEntityInformation<NamedUser, ?> second = new DummyJpaEntityInformation<NamedUser, Serializable>(NamedUser.class);
		assertThat(second.getEntityName()).isEqualTo("AnotherNamedUser");
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-93
	public void rejectsClassNotBeingFoundInMetamodel() {

		when(em.getMetamodel()).thenReturn(metaModel);
		JpaEntityInformationSupport.getEntityInformation(User.class, em);
	}

	static class User {

	}

	static class DummyJpaEntityInformation<T, ID> extends JpaEntityInformationSupport<T, ID> {

		public DummyJpaEntityInformation(Class<T> domainClass) {
			super(domainClass);
		}

		public SingularAttribute<? super T, ?> getIdAttribute() {
			return null;
		}

		public ID getId(T entity) {
			return null;
		}

		public Class<ID> getIdType() {
			return null;
		}

		public Iterable<String> getIdAttributeNames() {
			return Collections.emptySet();
		}

		public boolean hasCompositeId() {
			return false;
		}

		public Object getCompositeIdAttributeValue(Object id, String idAttribute) {
			return null;
		}
	}

	@Entity(name = "AnotherNamedUser")
	public class NamedUser {

	}
}
