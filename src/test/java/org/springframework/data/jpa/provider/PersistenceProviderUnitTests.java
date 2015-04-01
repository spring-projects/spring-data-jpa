/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.provider;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.data.jpa.provider.PersistenceProvider.ECLIPSELINK;
import static org.springframework.data.jpa.provider.PersistenceProvider.GENERIC_JPA;
import static org.springframework.data.jpa.provider.PersistenceProvider.HIBERNATE;
import static org.springframework.data.jpa.provider.PersistenceProvider.OPEN_JPA;
import static org.springframework.data.jpa.provider.PersistenceProvider.fromEntityManager;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.ECLIPSELINK_ENTITY_MANAGER_INTERFACE;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.HIBERNATE43_ENTITY_MANAGER_INTERFACE;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.HIBERNATE_ENTITY_MANAGER_INTERFACE;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.OPENJPA_ENTITY_MANAGER_INTERFACE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.Opcodes;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.query.JpaEntityGraph;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.util.ClassUtils;

/**
 * Tests for PersistenceProvider detection logic in {@link PersistenceProvider}.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
public class PersistenceProviderUnitTests {

	ShadowingClassLoader shadowingClassLoader;

	@Before
	public void setup() {
		this.shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader());
	}

	/**
	 * @see DATAJPA-444
	 */
	@Test
	public void detectsHibernatePersistenceProviderForHibernateVersionLessThan4Dot3() throws Exception {

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(HIBERNATE_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em), is(HIBERNATE));
	}

	/**
	 * @see DATAJPA-444
	 */
	@Test
	public void detectsHibernatePersistenceProviderForHibernateVersionGreaterEqual4dot3() throws Exception {

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(HIBERNATE43_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em), is(HIBERNATE));
	}

	@Test
	public void detectsOpenJpaPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.apache.openjpa.persistence");

		EntityManager em = mockProviderSpecificEntityManagerInterface(OPENJPA_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em), is(OPEN_JPA));
	}

	@Test
	public void detectsEclipseLinkPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(ECLIPSELINK_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em), is(ECLIPSELINK));
	}

	@Test
	public void fallbackToGenericJpaForUnknownPersistenceProvider() throws Exception {

		EntityManager em = mockProviderSpecificEntityManagerInterface("foo.bar.unknown.jpa.JpaEntityManager");

		assertThat(fromEntityManager(em), is(GENERIC_JPA));
	}

	/**
	 * @see DATAJPA-696
	 */
	@Test
	public void shouldBuildCorrectSubgraphForJpaEntityGraph() throws Exception {

		EntityGraph<?> entityGraph = mock(EntityGraph.class);
		Subgraph<?> subgraph = mock(Subgraph.class);
		doReturn(subgraph).when(entityGraph).addSubgraph(anyString());

		JpaEntityGraph jpaEntityGraph = new JpaEntityGraph("foo", EntityGraphType.FETCH,
				new String[] { "foo", "gugu.gaga" });

		PersistenceProvider.GENERIC_JPA.configureFetchGraphFrom(jpaEntityGraph, entityGraph);

		verify(entityGraph, times(1)).addAttributeNodes("foo");
		verify(entityGraph, times(1)).addSubgraph("gugu");
		verify(subgraph, times(1)).addAttributeNodes("gaga");
	}

	private EntityManager mockProviderSpecificEntityManagerInterface(String interfaceName) throws ClassNotFoundException {

		Class<?> providerSpecificEntityManagerInterface = InterfaceGenerator.generate(interfaceName, shadowingClassLoader,
				EntityManager.class);

		EntityManager em = EntityManager.class.cast(Mockito.mock(providerSpecificEntityManagerInterface));
		Mockito.when(em.getDelegate()).thenReturn(em); // delegate is used to determine the classloader of the provider
																										// specific interface, therefore we return the proxied
																										// EntityManager.

		return em;
	}

	static class InterfaceGenerator implements Opcodes {

		public static Class<?> generate(final String interfaceName, ClassLoader parentClassLoader,
				final Class<?>... interfaces) throws ClassNotFoundException {

			class CustomClassLoader extends ClassLoader {

				public CustomClassLoader(ClassLoader parent) {
					super(parent);
				}

				@Override
				protected Class<?> findClass(String name) throws ClassNotFoundException {

					if (name.equals(interfaceName)) {

						byte[] byteCode = generateByteCodeForInterface(interfaceName, interfaces);
						return defineClass(name, byteCode, 0, byteCode.length);
					}

					return super.findClass(name);
				}
			}

			return new CustomClassLoader(parentClassLoader).loadClass(interfaceName);
		}

		private static byte[] generateByteCodeForInterface(final String interfaceName, Class<?>... interfacesToImplement) {

			String interfaceResourcePath = ClassUtils.convertClassNameToResourcePath(interfaceName);

			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, interfaceResourcePath, null, "java/lang/Object",
					toResourcePaths(interfacesToImplement));
			cw.visitSource(interfaceResourcePath + ".java", null);
			cw.visitEnd();

			return cw.toByteArray();
		}

		private static String[] toResourcePaths(Class<?>... interfacesToImplement) {

			List<String> interfaceResourcePaths = new ArrayList<String>(interfacesToImplement.length);
			for (Class<?> iface : interfacesToImplement) {
				interfaceResourcePaths.add(ClassUtils.convertClassNameToResourcePath(iface.getName()));
			}

			return interfaceResourcePaths.toArray(new String[interfaceResourcePaths.size()]);
		}
	}
}
