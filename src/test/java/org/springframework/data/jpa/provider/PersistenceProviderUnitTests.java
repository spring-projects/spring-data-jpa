/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.provider;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.*;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.Version;
import org.junit.Assume;
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
 * @author Oliver Gierke
 * @author Jens Schauder
 */
public class PersistenceProviderUnitTests {

	ShadowingClassLoader shadowingClassLoader;

	@Before
	public void setup() {

		PersistenceProvider.CACHE.clear();

		this.shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader());
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

	@Test // DATAJPA-1019
	public void detectsHibernatePersistenceProviderForHibernateVersion52() throws Exception {

		Assume.assumeThat(Version.getVersionString(), startsWith("5.2"));

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(HIBERNATE_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em), is(HIBERNATE));
	}

	@Test // DATAJPA-1379
	public void detectsProviderFromProxiedEntityManager() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(ECLIPSELINK_ENTITY_MANAGER_INTERFACE);

		EntityManager emProxy = Mockito.mock(EntityManager.class);
		Mockito.when(emProxy.getDelegate()).thenReturn(em);

		assertThat(fromEntityManager(emProxy), is(ECLIPSELINK));
	}

	private EntityManager mockProviderSpecificEntityManagerInterface(String interfaceName) throws ClassNotFoundException {

		Class<?> providerSpecificEntityManagerInterface = InterfaceGenerator.generate(interfaceName, shadowingClassLoader,
				EntityManager.class);

		EntityManager em = (EntityManager) Mockito.mock(providerSpecificEntityManagerInterface);
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

			List<String> interfaceResourcePaths = new ArrayList<>(interfacesToImplement.length);
			for (Class<?> iface : interfacesToImplement) {
				interfaceResourcePaths.add(ClassUtils.convertClassNameToResourcePath(iface.getName()));
			}

			return interfaceResourcePaths.toArray(new String[0]);
		}
	}
}
