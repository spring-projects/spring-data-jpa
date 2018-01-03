/*
 * Copyright 2008-2018 the original author or authors.
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

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.springframework.util.Assert;

@Embeddable
public class SampleEntityPK implements Serializable {

	private static final long serialVersionUID = 231060947L;

	@Column(nullable = false)
	private String first;
	@Column(nullable = false)
	private String second;

	public SampleEntityPK() {

		this.first = null;
		this.second = null;
	}

	public SampleEntityPK(String first, String second) {

		Assert.notNull(first, "First must not be null!");
		Assert.notNull(second, "Second must not be null!");
		this.first = first;
		this.second = second;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		SampleEntityPK that = (SampleEntityPK) obj;

		return this.first.equals(that.first) && this.second.equals(that.second);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int result = 17;
		result += 31 * first.hashCode();
		result += 31 * second.hashCode();
		return result;
	}
}
