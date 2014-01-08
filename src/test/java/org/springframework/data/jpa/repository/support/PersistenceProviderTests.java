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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.asm.ClassWriter;
import org.springframework.asm.Opcodes;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.util.ClassUtils;

/**
 * Tests for PersistenceProvider detection logic in {@link PersistenceProvider}.
 * 
 * @author Thomas Darimont
 */
public class PersistenceProviderTests {

	private ShadowingClassLoader shadowingClassLoader;

	@Before
	public void setup() {
		shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader());
	}

	/**
	 * @see DATAJPA-444
	 */
	@Test
	public void detectsHibernatePersistenceProviderForHibernateVersionLessThan4dot3() throws Exception {

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(PersistenceProvider.Constants.HIBERNATE_ENTITY_MANAGER_INTERFACE);

		assertThat(PersistenceProvider.fromEntityManager(em), is(PersistenceProvider.HIBERNATE));
	}

	/**
	 * @see DATAJPA-444
	 */
	@Test
	public void detectsHibernatePersistenceProviderForHibernateVersionGreaterEqual4dot3() throws Exception {

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(PersistenceProvider.Constants.HIBERNATE43_ENTITY_MANAGER_INTERFACE);

		assertThat(PersistenceProvider.fromEntityManager(em), is(PersistenceProvider.HIBERNATE));
	}

	@Test
	public void detectsOpenJPAPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.apache.openjpa.persistence");

		EntityManager em = mockProviderSpecificEntityManagerInterface(PersistenceProvider.Constants.OPENJPA_ENTITY_MANAGER_INTERFACE);

		assertThat(PersistenceProvider.fromEntityManager(em), is(PersistenceProvider.OPEN_JPA));
	}

	@Test
	public void detectsEclipseLinkPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(PersistenceProvider.Constants.ECLIPSELINK_ENTITY_MANAGER_INTERFACE);

		assertThat(PersistenceProvider.fromEntityManager(em), is(PersistenceProvider.ECLIPSELINK));
	}

	@Test
	public void fallbackToGenericJpaForUnknownPersistenceProvider() throws Exception {

		EntityManager em = mockProviderSpecificEntityManagerInterface("foo.bar.unknown.jpa.JpaEntityManager");

		assertThat(PersistenceProvider.fromEntityManager(em), is(PersistenceProvider.GENERIC_JPA));
	}

	/**
	 * @param interfaceName
	 * @return
	 * @throws ClassNotFoundException
	 */
	private EntityManager mockProviderSpecificEntityManagerInterface(String interfaceName) throws ClassNotFoundException {

		Class<?> providerSpecificEntityManagerInterface = InterfaceGenerator.generate(interfaceName, shadowingClassLoader,
				EntityManager.class);

		EntityManager em = EntityManager.class.cast(Mockito.mock(providerSpecificEntityManagerInterface));
		Mockito.when(em.getDelegate()).thenReturn(em);

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

		private static byte[] generateByteCodeForInterface(final String interfaceName, Class<?>... interfaces) {

			String interfaceResourcePath = ClassUtils.convertClassNameToResourcePath(interfaceName);
			ClassWriter cw = new ClassWriter(0);
			List<String> interfaceResourcePaths = new ArrayList<String>(interfaces.length);
			for (Class<?> iface : interfaces) {
				interfaceResourcePaths.add(ClassUtils.convertClassNameToResourcePath(iface.getName()));
			}
			cw.visit(V1_6, ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, interfaceResourcePath, null, "java/lang/Object",
					interfaceResourcePaths.toArray(new String[interfaceResourcePaths.size()]));
			cw.visitSource(interfaceResourcePath + ".java", null);
			cw.visitEnd();

			return cw.toByteArray();
		}
	}
}
