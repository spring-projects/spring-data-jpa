/*
 * Copyright 2018-2019 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DefaultQueryHints}.
 *
 * @author Jens Schauder
 */
public class DefaultQueryHintsTest {

	JpaEntityInformation<?, ?> information = mock(JpaEntityInformation.class);
	CrudMethodMetadata metadata = mock(CrudMethodMetadata.class);

	@Before
	public void before() {

		setupMainHints();
		setUpCountHints();
	}

	@Test // DATAJPA-1156
	public void mainHints() {

		QueryHints hints = DefaultQueryHints.of(information, metadata);

		assertThat(hints.asMap()) //
				.extracting("name1", "name2", "n1", "n2") //
				.containsExactly("value1", "value2", null, null);
	}

	@Test // DATAJPA-1156
	public void countHints() {

		QueryHints hints = DefaultQueryHints.of(information, metadata).forCounts();

		assertThat(hints.asMap()) //
				.extracting("name1", "name2", "n1", "n2") //
				.containsExactly(null, null, "1", "2");
	}

	private void setupMainHints() {

		Map<String, Object> mainHintMap = new HashMap<>();
		mainHintMap.put("name1", "value1");
		mainHintMap.put("name2", "value2");

		when(metadata.getQueryHints()).thenReturn(mainHintMap);
	}

	private void setUpCountHints() {

		Map<String, Object> countHintMap = new HashMap<>();
		countHintMap.put("n1", "1");
		countHintMap.put("n2", "2");

		when(metadata.getQueryHintsForCount()).thenReturn(countHintMap);
	}
}
