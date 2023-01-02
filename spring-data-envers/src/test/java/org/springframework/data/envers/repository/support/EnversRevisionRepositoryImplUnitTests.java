/*
 * Copyright 2020-2023 the original author or authors.
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
package org.springframework.data.envers.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.Test;
import org.springframework.data.history.AnnotationRevisionMetadata;
import org.springframework.data.history.RevisionMetadata;

/**
 * Unit tests for {@link EnversRevisionRepositoryImpl}.
 *
 * @author Jens Schauder
 */
class EnversRevisionRepositoryImplUnitTests {

	@Test // gh-215
	void revisionTypeOfAnnotationRevisionMetadataIsProperlySet() {

		Object[] data = new Object[] { "a", "some metadata", RevisionType.DEL };

		EnversRevisionRepositoryImpl.QueryResult<Object> result = new EnversRevisionRepositoryImpl.QueryResult<>(data);

		RevisionMetadata<?> revisionMetadata = result.createRevisionMetadata();

		assertThat(revisionMetadata).isInstanceOf(AnnotationRevisionMetadata.class);
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.DELETE);
	}

	@Test // gh-215
	void revisionTypeOfDefaultRevisionMetadataIsProperlySet() {

		Object[] data = new Object[] { "a", mock(DefaultRevisionEntity.class), RevisionType.DEL };

		EnversRevisionRepositoryImpl.QueryResult<Object> result = new EnversRevisionRepositoryImpl.QueryResult<>(data);

		RevisionMetadata<?> revisionMetadata = result.createRevisionMetadata();

		assertThat(revisionMetadata).isInstanceOf(DefaultRevisionMetadata.class);
		assertThat(revisionMetadata.getRevisionType()).isEqualTo(RevisionMetadata.RevisionType.DELETE);
	}

}
