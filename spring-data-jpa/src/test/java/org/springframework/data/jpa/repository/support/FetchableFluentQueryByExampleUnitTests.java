/*
 * Copyright 2022-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.support.FetchableFluentQueryByExample;

/**
 * Unit tests for {@link FetchableFluentQueryByExample}.
 *
 * @author J.R. Onyschak
 */
class FetchableFluentQueryByExampleUnitTests {

	@Test // GH-2438
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void multipleSortBy() {

		Sort s1 = Sort.by(Order.by("s1"));
		Sort s2 = Sort.by(Order.by("s2"));
		FetchableFluentQueryByExample f = new FetchableFluentQueryByExample(Example.of(""), null, null, null, null, null);
		f = (FetchableFluentQueryByExample) f.sortBy(s1).sortBy(s2);
		assertThat(f.sort).isEqualTo(s1.and(s2));
	}
}
