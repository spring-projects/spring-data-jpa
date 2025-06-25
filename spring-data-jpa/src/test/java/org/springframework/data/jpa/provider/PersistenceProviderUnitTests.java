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
import static org.mockito.Mockito.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.*;
import static org.springframework.data.jpa.provider.PersistenceProvider.Constants.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.Opcodes;
import org.springframework.instrument.classloading.ShadowingClassLoader;
import org.springframework.orm.jpa.AbstractEntityManagerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

/**
 * Tests for PersistenceProvider detection logic in {@link PersistenceProvider}.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
class PersistenceProviderUnitTests {

	private ShadowingClassLoader shadowingClassLoader;

	@BeforeEach
	void setup() {

		Map<?, ?> cache = (Map<?, ?>) ReflectionTestUtils.getField(PersistenceProvider.class, "CACHE");
		cache.clear();

		this.shadowingClassLoader = new ShadowingClassLoader(getClass().getClassLoader());
	}

	@ParameterizedTest // GH-3425
	@EnumSource(PersistenceProvider.class)
	void entityManagerFactoryClassNamesAreInterfaces(PersistenceProvider provider) throws ClassNotFoundException {

		for (String className : provider.entityManagerFactoryClassNames) {
			assertThat(ClassUtils.forName(className, PersistenceProvider.class.getClassLoader()).isInterface()).isTrue();
		}
	}

	@ParameterizedTest // GH-3425
	@EnumSource(PersistenceProvider.class)
	void metaModelNamesExist(PersistenceProvider provider) throws ClassNotFoundException {

		for (String className : provider.entityManagerFactoryClassNames) {
			assertThat(ClassUtils.forName(className, PersistenceProvider.class.getClassLoader()).isInterface()).isNotNull();
		}
	}

	@Test
	void detectsEclipseLinkPersistenceProvider() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager em = mockProviderSpecificEntityManagerInterface(ECLIPSELINK_ENTITY_MANAGER_INTERFACE);
		when(em.getEntityManagerFactory())
				.thenReturn(mockProviderSpecificEntityManagerFactoryInterface(ECLIPSELINK_ENTITY_MANAGER_FACTORY_INTERFACE));

		assertThat(fromEntityManager(em)).isEqualTo(ECLIPSELINK);
	}

	@Test
	void fallbackToGenericJpaForUnknownPersistenceProvider() throws Exception {

		EntityManager em = mockProviderSpecificEntityManagerInterface("foo.bar.unknown.jpa.JpaEntityManager");
		when(em.getEntityManagerFactory()).thenReturn(mock(EntityManagerFactory.class));

		assertThat(fromEntityManager(em)).isEqualTo(GENERIC_JPA);
	}

	@Test // DATAJPA-1379
	void detectsProviderFromProxiedEntityManager() throws Exception {

		shadowingClassLoader.excludePackage("org.eclipse.persistence.jpa");

		EntityManager emProxy = Mockito.mock(EntityManager.class);
		when(emProxy.getEntityManagerFactory())
				.thenReturn(mockProviderSpecificEntityManagerFactoryInterface(ECLIPSELINK_ENTITY_MANAGER_FACTORY_INTERFACE));

		assertThat(fromEntityManager(emProxy)).isEqualTo(ECLIPSELINK);
	}

	@Test // GH-3923
	void detectsEntityManagerFromProxiedEntityManagerFactory() throws Exception {

		EntityManagerFactory emf = mockProviderSpecificEntityManagerFactoryInterface(
				"foo.bar.unknown.jpa.JpaEntityManager");
		when(emf.unwrap(null)).thenThrow(new NullPointerException());
		when(emf.unwrap(EntityManagerFactory.class)).thenReturn(emf);

		MyEntityManagerFactoryBean factoryBean = new MyEntityManagerFactoryBean(EntityManagerFactory.class, emf);
		EntityManagerFactory springProxy = factoryBean.createEntityManagerFactoryProxy(emf);

		Object externalProxy = Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class[] { EntityManagerFactory.class }, (proxy, method, args) -> method.invoke(emf, args));

		assertThat(PersistenceProvider.fromEntityManagerFactory(springProxy)).isEqualTo(GENERIC_JPA);
		assertThat(PersistenceProvider.fromEntityManagerFactory((EntityManagerFactory) externalProxy))
				.isEqualTo(GENERIC_JPA);
	}

	private EntityManager mockProviderSpecificEntityManagerInterface(String interfaceName) throws ClassNotFoundException {

		Class<?> providerSpecificEntityManagerInterface = InterfaceGenerator.generate(interfaceName, shadowingClassLoader,
				EntityManager.class);

		EntityManager em = (EntityManager) Mockito.mock(providerSpecificEntityManagerInterface);

		// delegate is used to determine the classloader of the provider
		// specific interface, therefore we return the proxied EntityManager
		when(em.getDelegate()).thenReturn(em);

		return em;
	}

	private EntityManagerFactory mockProviderSpecificEntityManagerFactoryInterface(String interfaceName)
			throws ClassNotFoundException {

		Class<?> providerSpecificEntityManagerInterface = InterfaceGenerator.generate(interfaceName, shadowingClassLoader,
				EntityManagerFactory.class);

		return (EntityManagerFactory) Mockito.mock(providerSpecificEntityManagerInterface);
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

	static class MyEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean {

		public MyEntityManagerFactoryBean(Class<? extends EntityManagerFactory> entityManagerFactoryInterface,
				EntityManagerFactory entityManagerFactory) {
			setEntityManagerFactoryInterface(entityManagerFactoryInterface);
			ReflectionTestUtils.setField(this, "nativeEntityManagerFactory", entityManagerFactory);

		}

		@Override
		protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
			return null;
		}

		@Override
		protected EntityManagerFactory createEntityManagerFactoryProxy(EntityManagerFactory emf) {
			return super.createEntityManagerFactoryProxy(emf);
		}
	}
}
