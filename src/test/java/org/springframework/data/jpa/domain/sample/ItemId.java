/*
 * Copyright 2016-2019 the original author or authors.
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

import java.io.Serializable;

/**
 * @author Mark Paluch
 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">Final JPA 2.1
 *      Specification 2.4.1.3 Derived Identities Example 2</a>
 */
public class ItemId implements Serializable {

	private static final long serialVersionUID = -2986871112875450036L;

	Integer id;
	Integer manufacturerId;

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
		if (!(o instanceof ItemId))
			return false;

		ItemId itemId = (ItemId) o;

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
