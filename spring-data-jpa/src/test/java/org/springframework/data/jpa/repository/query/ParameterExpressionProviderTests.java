/*
 * Copyright 2017-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.ParameterExpression;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.repository.query.DefaultParameters;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link ParameterMetadataProvider}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class ParameterExpressionProviderTests {

	@PersistenceContext EntityManager em;

	@Test // DATADOC-99
	@SuppressWarnings("rawtypes")
	void createsParameterExpressionWithMostConcreteType() throws Exception {

		Method method = SampleRepository.class.getMethod("findByIdGreaterThan", int.class);
		Parameters<?, ?> parameters = new DefaultParameters(method);
		ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, new Object[] { 1 });
		Part part = new Part("IdGreaterThan", User.class);

		CriteriaBuilder builder = em.getCriteriaBuilder();
		ParameterMetadataProvider provider = new ParameterMetadataProvider(builder, accessor, EscapeCharacter.DEFAULT);
		ParameterExpression<? extends Comparable> expression = provider.next(part, Comparable.class).getExpression();

		assertThat(expression.getParameterType()).isEqualTo(Integer.class);
	}

	interface SampleRepository {

		User findByIdGreaterThan(int id);
	}
}
