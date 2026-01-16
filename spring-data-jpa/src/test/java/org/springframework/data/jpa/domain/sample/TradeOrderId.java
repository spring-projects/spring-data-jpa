/*
 * Copyright 2016-present the original author or authors.
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
package org.springframework.data.jpa.domain.sample;

import java.util.Objects;

/**
 * @author James Bodkin
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#examples-of-derived-identities">Jakarta
 *      Persistence Specification: Derived Identities, Example 2</a>
 */
public class TradeOrderId {

	private Integer trade;
	private Integer number;

	public TradeOrderId() {}

	public TradeOrderId(Integer trade, Integer number) {
		this.trade = trade;
		this.number = number;
	}

	public Integer getTrade() {
		return trade;
	}

	public void setTrade(Integer trade) {
		this.trade = trade;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		TradeOrderId that = (TradeOrderId) o;
		return Objects.equals(trade, that.trade) && Objects.equals(number, that.number);
	}

	@Override
	public int hashCode() {
		return Objects.hash(trade, number);
	}

}
