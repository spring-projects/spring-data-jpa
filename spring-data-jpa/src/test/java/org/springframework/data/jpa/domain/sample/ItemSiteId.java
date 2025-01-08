/*
 * Copyright 2016-2025 the original author or authors.
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
public class ItemSiteId implements Serializable {

	@Serial private static final long serialVersionUID = 1822540289216799357L;

	private ItemId item;
	private Integer site;

	public ItemSiteId() {}

	public ItemSiteId(ItemId item, Integer site) {
		this.item = item;
		this.site = site;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ItemSiteId that))
			return false;

		if (item != null ? !item.equals(that.item) : that.item != null)
			return false;
		return site != null ? site.equals(that.site) : that.site == null;
	}

	@Override
	public int hashCode() {
		int result = item != null ? item.hashCode() : 0;
		result = 31 * result + (site != null ? site.hashCode() : 0);
		return result;
	}
}
