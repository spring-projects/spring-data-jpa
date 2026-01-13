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

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Repositories using hierarchical {@link jakarta.persistence.IdClass} identifiers.
 * 
 * @author James Bodkin
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HierarchicalIdClassRepositoryTests.TestConfig.class)
@Transactional
public class HierarchicalIdClassRepositoryTests {
	
	@Autowired private TradeRepository tradeRepository;

	@Autowired private TradeItemRepository tradeItemRepository;

	@Test
	void shouldScrollWithKeyset() {

		List<TradeOrder> tradeOrders = new ArrayList<>();
		List<TradeItem> tradeItems = new ArrayList<>();

		Trade trade = new Trade(tradeOrders);

		TradeOrder tradeOrder = new TradeOrder(trade, 1, tradeItems);
		tradeOrders.add(tradeOrder);

		TradeItem tradeItem1 = new TradeItem(tradeOrder, 1, null);
		tradeItems.add(tradeItem1);

		TradeItem tradeItem2 = new TradeItem(tradeOrder, 2, "PHYSICAL");
		tradeItems.add(tradeItem2);

		tradeRepository.saveAndFlush(trade);

		Window<TradeItem> first = tradeItemRepository.findBy(Specification.unrestricted(),
				q -> q.limit(1).sortBy(Sort.by("type")).scroll(ScrollPosition.keyset()));

		assertThat(first).containsOnly(tradeItem1);

		Window<TradeItem> next = tradeItemRepository.findBy(Specification.unrestricted(),
				q -> q.limit(1).sortBy(Sort.by("type")).scroll(first.positionAt(0)));

		assertThat(next).containsOnly(tradeItem2);
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = SampleConfig.class)
	static abstract class Config {

	}

	@ImportResource("classpath:infrastructure.xml")
	static class TestConfig extends Config {}
	
}
