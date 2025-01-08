/*
 * Copyright 2014-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.*;

import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assumptions;
import org.hibernate.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.Opcodes;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * Tests for PersistenceProvider detection logic in {@link PersistenceProvider}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 */
class PersistenceProviderUnitTests {

	private ShadowingClassLoader shadowingClassLoader;

	@BeforeEach
	void setup() {

		Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(PersistenceProvider.class, "CACHE");
		cache.clear();

		this.shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader());
	}

	@Test
	void detectsEclipseLinkPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(ECLIPSELINK_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em)).isEqualTo(ECLIPSELINK);
	}

	@Test
	void fallbackToGenericJpaForUnknownPersistenceProvider() throws Exception {

		EntityManager em = mockProviderSpecificEntityManagerInterface("foo.bar.unknown.jpa.JpaEntityManager");

		assertThat(fromEntityManager(em)).isEqualTo(GENERIC_JPA);
	}

	@Test // DATAJPA-1019
	void detectsHibernatePersistenceProviderForHibernateVersion52() throws Exception {

		Assumptions.assumeThat(Version.getVersionString()).startsWith("5.2");

		shadowingClassLoader.excludePackage("org.hibernate");

		EntityManager em = mockProviderSpecificEntityManagerInterface(HIBERNATE_ENTITY_MANAGER_INTERFACE);

		assertThat(fromEntityManager(em)).isEqualTo(HIBERNATE);
	}

	@Test // DATAJPA-1379
	void detectsProviderFromProxiedEntityManager() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(ECLIPSELINK_ENTITY_MANAGER_INTERFACE);

		EntityManager emProxy = Mockito.mock(EntityManager.class);
		Mockito.when(emProxy.getDelegate()).thenReturn(em);

		assertThat(fromEntityManager(emProxy)).isEqualTo(ECLIPSELINK);
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

		static Class<?> generate(final String interfaceName, ClassLoader parentClassLoader, final Class<?>... interfaces)
				throws ClassNotFoundException {

			class CustomClassLoader extends ClassLoader {

				private CustomClassLoader(ClassLoader parent) {
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

			return Arrays.stream(interfacesToImplement) //
					.map(Class::getName) //
					.map(ClassUtils::convertClassNameToResourcePath) //
					.toArray(String[]::new);
		}
	}
}
