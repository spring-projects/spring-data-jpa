/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.cdi;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.ProcessBean;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link JpaRepositoryExtension}.
 *
 * @author Oliver Gierke
 */
public class JpaRepositoryExtensionUnitTests {

	Bean<EntityManager> em, alternativeEm;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		Set<Type> types = Collections.singleton((Type) EntityManager.class);

		em = mock(Bean.class);
		when(em.getTypes()).thenReturn(types);

		alternativeEm = mock(Bean.class);
		when(alternativeEm.getTypes()).thenReturn(types);
		when(alternativeEm.isAlternative()).thenReturn(true);

	}

	@Test
	public void registersEntityManager() {

		JpaRepositoryExtension extension = new JpaRepositoryExtension();
		extension.processBean(createEntityManagerBeanMock(em));

		assertEntityManagerRegistered(extension, em);
	}

	@Test // DATAJPA-388
	public void alternativeEntityManagerOverridesDefault() {

		JpaRepositoryExtension extension = new JpaRepositoryExtension();
		extension.processBean(createEntityManagerBeanMock(em));
		extension.processBean(createEntityManagerBeanMock(alternativeEm));

		assertEntityManagerRegistered(extension, alternativeEm);
	}

	@Test // DATAJPA-388
	public void alternativeEntityManagerDoesNotGetOverridden() {

		JpaRepositoryExtension extension = new JpaRepositoryExtension();
		extension.processBean(createEntityManagerBeanMock(alternativeEm));
		extension.processBean(createEntityManagerBeanMock(em));

		assertEntityManagerRegistered(extension, alternativeEm);
	}

	@SuppressWarnings("unchecked")
	private static void assertEntityManagerRegistered(JpaRepositoryExtension extension, Bean<EntityManager> em) {

		Map<Set<Annotation>, Bean<EntityManager>> entityManagers = (Map<Set<Annotation>, Bean<EntityManager>>) ReflectionTestUtils
				.getField(extension, "entityManagers");
		assertThat(entityManagers.size(), is(1));
		assertThat(entityManagers.values(), hasItem(em));
	}

	@SuppressWarnings("unchecked")
	private static ProcessBean<EntityManager> createEntityManagerBeanMock(Bean<EntityManager> bean) {

		ProcessBean<EntityManager> mock = mock(ProcessBean.class);
		when(mock.getBean()).thenReturn(bean);

		return mock;
	}
}
