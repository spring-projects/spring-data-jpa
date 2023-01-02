/*
 * Copyright 2021-2023 the original author or authors.
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

import java.io.Serializable;

/**
 * @author Mark Paluch
 */
public class PersistableWithSingleIdClassPK implements Serializable {

	private static final long serialVersionUID = 23126782341L;

	private Long first;

	public PersistableWithSingleIdClassPK() {

	}

	public PersistableWithSingleIdClassPK(Long first) {
		this.first = first;
	}

	public void setFirst(Long first) {
		this.first = first;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null || !(obj.getClass().equals(getClass()))) {
			return false;
		}

		PersistableWithSingleIdClassPK that = (PersistableWithSingleIdClassPK) obj;

		return nullSafeEquals(this.first, that.first);
	}

	@Override
	public int hashCode() {

		int result = 17;

		result += nullSafeHashCode(this.first);

		return result;
	}
}
