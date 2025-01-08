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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Mark Paluch
 * @author Oliver Gierke
 * @author Aleksei Elin
 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#examples-of-derived-identities">Jakarta
 *      Persistence Specification: Derived Identities, Example 2</a>
 */
@Entity
@Table
public class Site implements java.io.Serializable {

	@Serial private static final long serialVersionUID = 1L;

	@Id @GeneratedValue Integer id;

	public Site() {}

	public Site(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}
}
