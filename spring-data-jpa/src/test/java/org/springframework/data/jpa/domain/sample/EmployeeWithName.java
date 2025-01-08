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

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Greg Turnquist
 */
@Entity
public class EmployeeWithName {

	@Id
	@GeneratedValue //
	private Integer id;
	private String name;

	public EmployeeWithName(String name) {

		this();
		this.name = name;
	}

	protected EmployeeWithName() {}

	public Integer getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return "EmployeeWithName(id=" + this.getId() + ", name=" + this.getName() + ")";
	}
}
