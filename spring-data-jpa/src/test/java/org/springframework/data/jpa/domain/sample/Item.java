/*
 * Copyright 2016-2024 the original author or authors.
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * @author Mark Paluch
 * @author Aleksei Elin
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#examples-of-derived-identities">Jakarta Persistence
 *      Specification: Derived Identities, Example 2</a>
 */
@Entity
@Table
@IdClass(ItemId.class)
public class Item {

	@Id
	@Column(columnDefinition = "INT") private Integer id;

	@Id
	@JoinColumn(name = "manufacturer_id", columnDefinition = "INT") private Integer manufacturerId;

	private String name;

	public Item() {}

	public Item(Integer id, Integer manufacturerId) {
		this.id = id;
		this.manufacturerId = manufacturerId;
	}

	public Item(Integer id, Integer manufacturerId, String name) {

		this.id = id;
		this.manufacturerId = manufacturerId;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public Integer getManufacturerId() {
		return manufacturerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Item item = (Item) o;
		return Objects.equals(id, item.id) && Objects.equals(manufacturerId, item.manufacturerId)
				&& Objects.equals(name, item.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, manufacturerId, name);
	}

	public String toString() {
		return "Item(id=" + this.getId() + ", manufacturerId=" + this.getManufacturerId() + ", name=" + this.getName()
				+ ")";
	}
}
