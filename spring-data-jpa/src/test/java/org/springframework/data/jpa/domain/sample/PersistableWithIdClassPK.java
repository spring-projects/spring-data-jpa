/*
 * Copyright 2012-2025 the original author or authors.
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

import static org.springframework.util.ObjectUtils.*;

import java.io.Serial;
import java.io.Serializable;

/**
 *
 * @author Oliver Gierke
 */
public class PersistableWithIdClassPK implements Serializable {

	@Serial private static final long serialVersionUID = 23126782341L;

	private Long first;
	private Long second;

	public PersistableWithIdClassPK() {

	}

	public PersistableWithIdClassPK(Long first, Long second) {
		this.first = first;
		this.second = second;
	}

	public void setFirst(Long first) {
		this.first = first;
	}

	public void setSecond(Long second) {
		this.second = second;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || !(obj.getClass().equals(getClass()))) {
			return false;
		}

		PersistableWithIdClassPK that = (PersistableWithIdClassPK) obj;

		return nullSafeEquals(this.first, that.first) && nullSafeEquals(this.second, that.second);
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += nullSafeHashCode(this.first);
		result += nullSafeHashCode(this.second);

		return result;
	}
}
