/*
 * Copyright 2017-2019 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.core.types.EntityPath;

/**
 * Unit tests for {@link EntityPathResolver} related tests on {@link JpaRepositoryFactoryBean}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 */
public class JpaRepositoryFactoryBeanEntityPathResolverIntegrationTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(basePackageClasses = UserRepository.class, //
			includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = UserRepository.class))
	static class BaseConfig {

		static final EntityPathResolver RESOLVER = new EntityPathResolver() {

			@Override
			public <T> EntityPath<T> createPath(Class<T> domainClass) {
				return null;
			}
		};
	}

	@Configuration
	static class FirstEntityPathResolver {

		@Bean
		EntityPathResolver firstEntityPathResolver() {
			return BaseConfig.RESOLVER;
		}
	}

	@Configuration
	static class SecondEntityPathResolver {

		@Bean
		EntityPathResolver secondEntityPathResolver() {
			return BaseConfig.RESOLVER;
		}
	}

	@Test // DATAJPA-1234, DATAJPA-1394
	public void usesSimpleEntityPathResolverByDefault() {
		assertEntityPathResolver(SimpleEntityPathResolver.INSTANCE, BaseConfig.class);
	}

	@Test // DATAJPA-1234, DATAJPA-1394
	public void usesExplicitlyRegisteredEntityPathResolver() {
		assertEntityPathResolver(BaseConfig.RESOLVER, BaseConfig.class, FirstEntityPathResolver.class);
	}

	@Test // DATAJPA-1234, DATAJPA-1394
	public void rejectsMulitpleEntityPathResolvers() {

		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() -> {

			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BaseConfig.class,
					FirstEntityPathResolver.class, SecondEntityPathResolver.class);
			context.close();

		}).withCauseExactlyInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	private static void assertEntityPathResolver(EntityPathResolver resolver, Class<?>... configurations) {

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configurations)) {

			JpaRepositoryFactoryBean<?, ?, ?> factory = context.getBean("&userRepository", JpaRepositoryFactoryBean.class);

			assertThat(ReflectionTestUtils.getField(factory, "entityPathResolver")).isEqualTo(resolver);
		}
	}
}
