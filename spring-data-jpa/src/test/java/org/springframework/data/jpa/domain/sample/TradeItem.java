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
package org.springframework.data.jpa.domain.sample;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author James Bodkin
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#examples-of-derived-identities">Jakarta
 *      Persistence Specification: Derived Identities, Example 2</a>
 */
@Entity
@Table
@IdClass(TradeItemId.class)
public class TradeItem {

	@Id @ManyToOne TradeOrder tradeOrder;

	@Id Integer number;

	@Column String type;

	public TradeItem() {}

	public TradeItem(TradeOrder tradeOrder, Integer number, String type) {
		this.tradeOrder = tradeOrder;
		this.number = number;
		this.type = type;
	}

	public TradeOrder getTradeOrder() {
		return tradeOrder;
	}

	public Integer getNumber() {
		return number;
	}

	public String getType() {
		return type;
	}

}
