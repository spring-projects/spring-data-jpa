/*
 * Copyright 2019-2023 the original author or authors.
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
package org.springframework.data.jpa.repository;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.sample.ScrollableEntity;
import org.springframework.data.jpa.repository.sample.ScrollableEntityRepository;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Yanming Zhou
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:config/namespace-application-context.xml")
@Transactional
class KeysetScrollIntegrationTests {

	private static final int pageSize = 10;

	private static final String[][] sortKeys = new String[][] { null, { "id" }, { "seqNo" }, { "seqNo", "id" } };

	private static final Integer[] totals = new Integer[] { 0, 5, 10, 15, 20, 25 };

	@Autowired
	ScrollableEntityRepository repository;

	void prepare(int total) {
		for (int i = 0; i < total; i++) {
			ScrollableEntity entity = new ScrollableEntity();
			entity.setSeqNo(i);
			this.repository.save(entity);
		}
	}

	@ParameterizedTest
	@MethodSource("cartesian")
	void scroll(@Nullable String[] keys, Sort.Direction sortDirection, ScrollPosition.Direction scrollDirection, int total) {

		prepare(total);

		List<List<ScrollableEntity>> contents = new ArrayList<>();

		Sort sort;
		if (keys != null) {
			sort = Sort.by(sortDirection, keys);
		}
		else {
			sort = Sort.unsorted();
			// implicit "id:ASC" will be used
			assumeTrue(sortDirection == Sort.Direction.ASC);
		}
		KeysetScrollPosition position = ScrollPosition.of(Map.of(), scrollDirection);
		if (scrollDirection == ScrollPosition.Direction.BACKWARD && position.getDirection() == ScrollPosition.Direction.FORWARD) {
			// remove this workaround if https://github.com/spring-projects/spring-data-commons/pull/2841 merged
			position = position.backward();
		}
		while (true) {
			ScrollPosition positionToUse = position;
			Window<ScrollableEntity> window = this.repository.findBy((root, query, cb) -> null,
					q -> q.limit(pageSize).sortBy(sort).scroll(positionToUse));
			if (!window.isEmpty()) {
				contents.add(window.getContent());
			}
			if (!window.hasNext()) {
				break;
			}
			int indexForNext = position.scrollsForward() ? window.size() - 1 : 0;
			position = (KeysetScrollPosition) window.positionAt(indexForNext);
			// position = window.positionForNext(); https://github.com/spring-projects/spring-data-commons/pull/2843
		}

		if (total == 0) {
			assertThat(contents).hasSize(0);
			return;
		}

		boolean divisible = total % pageSize == 0;

		assertThat(contents).hasSize(divisible ? total / pageSize : total / pageSize + 1);
		for (int i = 0; i < contents.size() - 1; i++) {
			assertThat(contents.get(i)).hasSize(pageSize);
		}
		if (divisible) {
			assertThat(contents.get(contents.size() - 1)).hasSize(pageSize);
		}
		else {
			assertThat(contents.get(contents.size() - 1)).hasSize(total % pageSize);
		}

		List<ScrollableEntity> first = contents.get(0);
		List<ScrollableEntity> last = contents.get(contents.size() - 1);

		if (sortDirection == Sort.Direction.ASC) {
			if (scrollDirection == ScrollPosition.Direction.FORWARD) {
				assertThat(first.get(0).getSeqNo()).isEqualTo(0);
				assertThat(last.get(last.size() - 1).getSeqNo()).isEqualTo(total - 1);
			}
			else {
				assertThat(first.get(first.size() - 1).getSeqNo()).isEqualTo(total - 1);
				assertThat(last.get(0).getSeqNo()).isEqualTo(0);
			}
		}
		else {
			if (scrollDirection == ScrollPosition.Direction.FORWARD) {
				assertThat(first.get(0).getSeqNo()).isEqualTo(total - 1);
				assertThat(last.get(last.size() - 1).getSeqNo()).isEqualTo(0);
			}
			else {
				assertThat(first.get(first.size() - 1).getSeqNo()).isEqualTo(0);
				assertThat(last.get(0).getSeqNo()).isEqualTo(total - 1);
			}
		}
	}

	private static Stream<Arguments> cartesian() {
		return Stream.of(sortKeys)
				.flatMap(keys -> Stream.of(Sort.Direction.class.getEnumConstants())
						.flatMap(sortDirection -> Stream.of(ScrollPosition.Direction.class.getEnumConstants())
								.flatMap(scrollDirection -> Stream.of(totals)
										.map(total -> Arguments.of(keys, sortDirection, scrollDirection, total)))));
	}
}
