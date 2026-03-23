/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Trade;
import org.springframework.data.jpa.domain.sample.TradeItem;
import org.springframework.data.jpa.domain.sample.TradeOrder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.SampleConfig;
import org.springframework.data.jpa.repository.sample.TradeItemRepository;
import org.springframework.data.jpa.repository.sample.TradeRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for Repositories using hierarchical {@link jakarta.persistence.IdClass} identifiers.
 *
 * @author James Bodkin
 * @author Mark Paluch
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HierarchicalIdClassRepositoryTests.Config.class)
class HierarchicalIdClassRepositoryTests {

	@Autowired TradeRepository tradeRepository;

	@Autowired TradeItemRepository tradeItemRepository;

	Trade trade = new Trade(new ArrayList<>());
	TradeOrder tradeOrder = new TradeOrder(trade, 1, new ArrayList<>());
	TradeItem tradeItem1 = new TradeItem(tradeOrder, 1, "DIGITAL");
	TradeItem tradeItem2 = new TradeItem(tradeOrder, 2, "PHYSICAL");

	@BeforeEach
	void setUp() {

		trade.getTradeOrders().add(tradeOrder);

		tradeOrder.getTradeItems().add(tradeItem1);
		tradeOrder.getTradeItems().add(tradeItem2);

		tradeRepository.saveAndFlush(trade);
	}

	@ParameterizedTest
	@MethodSource("scrollFixtures") // GH-4156
	void shouldScrollWithKeysetAsc(Sort.Direction direction, int firstNumber, int secondNumber) {

		Window<TradeItem> first = tradeItemRepository.findBy(Specification.unrestricted(),
				q -> q.limit(1).sortBy(Sort.by(direction, "type")).scroll(ScrollPosition.keyset()));

		assertThat(first).extracting(TradeItem::getNumber).containsOnly(firstNumber);

		Window<TradeItem> next = tradeItemRepository.findBy(Specification.unrestricted(),
				q -> q.limit(1).sortBy(Sort.by(direction, "type")).scroll(first.positionAt(0)));

		assertThat(next).extracting(TradeItem::getNumber).containsOnly(secondNumber);
	}

	static Stream<Arguments> scrollFixtures() {
		return Stream.of(Arguments.argumentSet("asc", Sort.Direction.ASC, 1, 2),
				Arguments.argumentSet("desc", Sort.Direction.DESC, 2, 1));
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = SampleConfig.class)
	@ImportResource("classpath:hibernate-infrastructure.xml")
	static class Config {

	}

}
