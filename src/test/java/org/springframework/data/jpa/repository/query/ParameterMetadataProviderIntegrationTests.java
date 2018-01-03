/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for {@link ParameterMetadataProvider}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @soundtrack Elephants Crossing - We are (Irrelephant)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class ParameterMetadataProviderIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-758
	public void forwardsParameterNameIfTransparentlyNamed() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByFirstname", String.class));
		ParameterMetadata<Object> metadata = provider.next(new Part("firstname", User.class));

		assertThat(metadata.getExpression().getName()).isEqualTo("name");
	}

	@Test // DATAJPA-758
	public void forwardsParameterNameIfExplicitlyAnnotated() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByLastname", String.class));
		ParameterMetadata<Object> metadata = provider.next(new Part("lastname", User.class));

		assertThat(metadata.getExpression().getName()).isNull();
	}

	@Test // DATAJPA-772
	public void doesNotApplyLikeExpansionOnNonStringProperties() throws Exception {

		ParameterMetadataProvider provider = createProvider(Sample.class.getMethod("findByAgeContaining", Integer.class));
		ParameterMetadata<Object> metadata = provider.next(new Part("ageContaining", User.class));

		assertThat(metadata.prepare(1)).isEqualTo(1);
	}

	private ParameterMetadataProvider createProvider(Method method) {

		JpaParameters parameters = new JpaParameters(method);
		simulateDiscoveredParametername(parameters);

		return new ParameterMetadataProvider(em.getCriteriaBuilder(), parameters,
				PersistenceProvider.fromEntityManager(em));
	}

	@SuppressWarnings({ "unchecked", "ConstantConditions" })
	private static void simulateDiscoveredParametername(Parameters<?, ?> parameters) {

		List<Object> list = (List<Object>) ReflectionTestUtils.getField(parameters, "parameters");
		Object parameter = ReflectionTestUtils.getField(list.get(0), "parameter");
		ReflectionTestUtils.setField(parameter, "parameterName", "name");
	}

	interface Sample {

		User findByFirstname(@Param("name") String firstname);

		User findByLastname(String lastname);

		User findByAgeContaining(@Param("age") Integer age);
	}
}
