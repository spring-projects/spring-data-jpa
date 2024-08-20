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

import java.io.Serial;
import java.io.Serializable;

/**
 * @author Mark Paluch
 * @author Aleksei Elin
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#examples-of-derived-identities">Jakarta
 *      Persistence Specification: Derived Identities, Example 2</a>
 */
public class ItemId implements Serializable {

	@Serial private static final long serialVersionUID = -2986871112875450036L;

	private Integer id;
	private Integer manufacturerId;

	public ItemId() {}

	public ItemId(Integer id, Integer manufacturerId) {
		this.id = id;
		this.manufacturerId = manufacturerId;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(Integer manufacturerId) {
		this.manufacturerId = manufacturerId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ItemId itemId))
			return false;

		if (id != null ? !id.equals(itemId.id) : itemId.id != null)
			return false;
		return manufacturerId != null ? manufacturerId.equals(itemId.manufacturerId) : itemId.manufacturerId == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (manufacturerId != null ? manufacturerId.hashCode() : 0);
		return result;
	}
}
