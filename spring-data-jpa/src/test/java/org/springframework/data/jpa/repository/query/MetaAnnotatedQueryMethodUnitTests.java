/*
 * Copyright 2013-2023 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Meta;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Verify that {@link Meta}-annotated methods property capture comments.
 *
 * @author Greg Turnquist
 * @since 3.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MetaAnnotatedQueryMethodUnitTests {

	@Mock QueryExtractor extractor;

	private ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	@Test // GH-775
	void metaAnnotationCommentsAreCapturedInJpaQueryMethod() throws Exception {

		Method method = UserRepository.class.getMethod("metaMethod");
		DefaultRepositoryMetadata repositoryMetadata = new DefaultRepositoryMetadata(UserRepository.class);
		JpaQueryMethod jpaQueryMethod = new JpaQueryMethod(method, repositoryMetadata, factory, extractor);

		assertThat(jpaQueryMethod.getQueryMetaAttributes().getComment()).isEqualTo("Comments embedded in SQL");
	}

	interface UserRepository extends CrudRepository<User, String> {

		@Meta(comment = "Comments embedded in SQL")
		void metaMethod();
	}
}
