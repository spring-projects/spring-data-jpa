/*
 * Copyright 2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.Repository;

public class JpaRepositoryFactoryBeanEntityPathResolverUnitTests {

	ListableBeanFactory beanFactory = mock(ListableBeanFactory.class);

	JpaRepositoryFactoryBean<DummyRepository, DummyEntity, Long> factoryBean = new JpaRepositoryFactoryBean<>(
			DummyRepository.class);
	Map<String, EntityPathResolver> beans = new HashMap<>();
	SimpleEntityPathResolver pathResolver = new SimpleEntityPathResolver("aSuffix");

	@Before
	public void setup() {
		doReturn(beans).when(beanFactory).getBeansOfType(EntityPathResolver.class);
	}

	@Test
	public void withoutConfiguredBeanTheDefaultInstanceIsUsed() throws Exception {

		factoryBean.setBeanFactory(beanFactory);

		Object entityPathResolver = extractEntityPathResolver(factoryBean);

		assertThat(entityPathResolver).isEqualTo(SimpleEntityPathResolver.INSTANCE);
	}

	@Test
	public void aSingleBeanOfTypeEntityPathResolverIsUsed() throws Exception {

		beans.put("irrelevant", pathResolver);

		factoryBean.setBeanFactory(beanFactory);

		Object entityPathResolver = extractEntityPathResolver(factoryBean);

		assertThat(entityPathResolver).isEqualTo(pathResolver);
	}

	@Test
	public void anEntityPathResolverWithProperNameIsUsed() throws Exception {

		beans.put("entityPathResolver", pathResolver);
		beans.put("irrelevant", new SimpleEntityPathResolver("anotherSuffix"));

		factoryBean.setBeanFactory(beanFactory);

		Object entityPathResolver = extractEntityPathResolver(factoryBean);

		assertThat(entityPathResolver).isEqualTo(pathResolver);
	}

	@Test
	public void withMultipleEntityPathResolversAnExceptionIsThrown() throws Exception {

		beans.put("irrelevant", pathResolver);
		beans.put("other", new SimpleEntityPathResolver("anotherSuffix"));

		assertThatExceptionOfType(NoUniqueBeanDefinitionException.class) //
				.isThrownBy(() -> factoryBean.setBeanFactory(beanFactory));

	}

	private Object extractEntityPathResolver(JpaRepositoryFactoryBean<DummyRepository, DummyEntity, Long> factoryBean)
			throws Exception {

		Field pathResolverField = JpaRepositoryFactoryBean.class.getDeclaredField("entityPathResolver");
		pathResolverField.setAccessible(true);
		return pathResolverField.get(factoryBean);
	}

	static class DummyEntity {}

	interface DummyRepository extends Repository<DummyEntity, Long> {}

}
