/*
 * Copyright 2008-2024 the original author or authors.
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

import java.util.Optional;

import org.springframework.lang.Nullable;

/**
 * @author Greg Turnquist
 */
@Entity
public class UserWithOptionalField {

	@Id
	@GeneratedValue private Long id;
	private String name;
	private String role;

	public UserWithOptionalField() {

		this.id = null;
		this.name = null;
		this.role = null;
	}

	public UserWithOptionalField(String name, @Nullable String role) {

		this();
		this.name = name;
		this.role = role;
	}

	public Optional<String> getRole() {
		return Optional.ofNullable(this.role);
	}

	public void setRole(Optional<String> role) {
		this.role = role.orElse(null);
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return "UserWithOptionalField(id=" + this.getId() + ", name=" + this.getName() + ", role=" + this.getRole() + ")";
	}
}
