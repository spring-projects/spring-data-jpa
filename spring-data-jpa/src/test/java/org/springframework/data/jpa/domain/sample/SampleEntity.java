/*
 * Copyright 2008-2025 the original author or authors.
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

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Oliver Gierke
 */
@Entity
public class SampleEntity {

	@EmbeddedId
	private SampleEntityPK id;

	protected SampleEntity() {

	}

	public SampleEntity(String first, String second) {

		this.id = new SampleEntityPK(first, second);
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!getClass().equals(obj.getClass())) {
			return false;
		}

		SampleEntity that = (SampleEntity) obj;

		return this.id.equals(that.id);
	}

	@Override
	public int hashCode() {

		return id.hashCode();
	}
}
