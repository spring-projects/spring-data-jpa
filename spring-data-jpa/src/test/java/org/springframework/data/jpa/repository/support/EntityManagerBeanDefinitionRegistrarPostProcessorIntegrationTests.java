/*
 * Copyright 2014-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link EntityManagerBeanDefinitionRegistrarPostProcessor}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Réda Housni Alaoui
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
class EntityManagerBeanDefinitionRegistrarPostProcessorIntegrationTests {

	@Autowired EntityManagerInjectionTarget target;

	@Test // DATAJPA-445
	void injectsEntityManagerIntoConstructors() {

		assertThat(target).isNotNull();
		assertThat(target.firstEm).isNotNull();
		assertThat(target.primaryEm).isNotNull();
	}

	@Configuration
	@Import(EntityManagerInjectionTarget.class)
	@ImportResource("classpath:infrastructure.xml")
	static class Config {

		@Autowired @Qualifier("entityManagerFactory") EntityManagerFactory emf;

		@Bean
		public static EntityManagerBeanDefinitionRegistrarPostProcessor processor() {
			return new EntityManagerBeanDefinitionRegistrarPostProcessor();
		}

		@Bean
		EntityManagerFactory firstEmf() {
			return emf;
		}

		@Bean
		EntityManagerFactory secondEmf() {
			return emf;
		}

		@Primary
		@Bean
		EntityManagerFactory thirdEmf() {
			return emf;
		}
	}

	static class EntityManagerInjectionTarget {

		private final EntityManager firstEm;
		private final EntityManager primaryEm;

		@Autowired
		public EntityManagerInjectionTarget(@Qualifier("firstEmf") EntityManager firstEm, EntityManager primaryEm) {

			this.firstEm = firstEm;
			this.primaryEm = primaryEm;
		}
	}
}
