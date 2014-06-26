/*
 * Copyright 2008-2014 the original author or authors.
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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.springframework.util.ObjectUtils;

/**
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Entity
public class SampleEntity {

	@EmbeddedId protected SampleEntityPK id;

	protected String attribute1;

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

		return this.id.equals(that.id) && ObjectUtils.nullSafeEquals(this.attribute1, that.attribute1);
	}

	public String getAttribute1() {
		return attribute1;
	}

	public void setAttribute1(String attribute1) {
		this.attribute1 = attribute1;
	}

	@Override
	public int hashCode() {
		return 17 * ObjectUtils.nullSafeHashCode(id) + ObjectUtils.nullSafeHashCode(attribute1);
	}
}
