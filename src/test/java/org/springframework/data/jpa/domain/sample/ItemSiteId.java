/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
public class ItemSiteId implements Serializable {

	private static final long serialVersionUID = 1822540289216799357L;

	ItemId item;
	Integer site;

	public ItemSiteId() {}

	public ItemSiteId(ItemId item, Integer site) {
		this.item = item;
		this.site = site;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ItemSiteId))
			return false;

		ItemSiteId that = (ItemSiteId) o;

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
