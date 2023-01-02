/*
 * Copyright 2016-2023 the original author or authors.
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

/**
 * @author Mark Paluch
 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">Final JPA 2.1
 *      Specification 2.4.1.3 Derived Identities Example 2</a>
 */
@Entity
@Table
@IdClass(ItemId.class)
public class Item {

	@Id @Column(columnDefinition = "INT") private Integer id;

	@Id @JoinColumn(name = "manufacturer_id", columnDefinition = "INT") private Integer manufacturerId;

	public Item() {}

	public Item(Integer id, Integer manufacturerId) {
		this.id = id;
		this.manufacturerId = manufacturerId;
	}

	public Integer getId() {
		return id;
	}

	public Integer getManufacturerId() {
		return manufacturerId;
	}
}
