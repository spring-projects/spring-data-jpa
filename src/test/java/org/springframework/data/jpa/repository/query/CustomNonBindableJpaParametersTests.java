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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.MethodParameter;
import org.springframework.data.jpa.domain.sample.Product;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Réda Housni Alaoui
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CustomNonBindableJpaParametersTests {

	@Autowired ProductRepository products;

	@Test
	public void test() {
		Product product = products.save(new Product());
		assertThat(products.findById(product.getId(), new NonBindable())).isNotEmpty();
	}

	private static class NonBindable {

	}

	public interface ProductRepository extends JpaRepository<Product, Long> {
		Optional<Product> findById(long id, NonBindable nonBindable);
	}

	private static class NonBindableAwareJpaParameter extends JpaParameters.JpaParameter {

		private final MethodParameter parameter;

		NonBindableAwareJpaParameter(MethodParameter parameter) {
			super(parameter);
			this.parameter = parameter;
		}

		@Override
		public boolean isBindable() {
			return !NonBindable.class.equals(parameter.getParameterType()) && super.isBindable();
		}
	}

	private static class NonBindableAwareJpaParameters extends JpaParameters {

		public NonBindableAwareJpaParameters(Method method) {
			super(method);
		}

		@Override
		protected JpaParameter createParameter(MethodParameter parameter) {
			return new NonBindableAwareJpaParameter(parameter);
		}
	}

	private static class NonBindableAwareJpaQueryMethod extends JpaQueryMethod {

		NonBindableAwareJpaQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				QueryExtractor extractor) {
			super(method, metadata, factory, extractor);
		}

		@Override
		protected JpaParameters createParameters(Method method) {
			return new NonBindableAwareJpaParameters(method);
		}
	}

	private static class NonBindableAwareJpaQueryMethodFactory implements JpaQueryMethodFactory {

		@Override
		public JpaQueryMethod build(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
				QueryExtractor extractor) {
			return new NonBindableAwareJpaQueryMethod(method, metadata, factory, extractor);
		}
	}

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	@EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = ProductRepository.class, //
			includeFilters = @ComponentScan.Filter(value = { ProductRepository.class }, type = FilterType.ASSIGNABLE_TYPE))
	static class Config {

		@Bean
		JpaQueryMethodFactory jpaQueryMethodFactory() {
			return new NonBindableAwareJpaQueryMethodFactory();
		}

	}

}
