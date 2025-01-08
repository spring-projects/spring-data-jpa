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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedQuery;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.jpa.domain.AbstractAuditable;
import org.springframework.lang.Nullable;

/**
 * Sample auditable user to demonstrate working with {@code AbstractAuditableEntity}. No declaration of an ID is
 * necessary. Furthermore no auditing information has to be declared explicitly.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Entity
@NamedQuery(name = "AuditableUser.findByFirstname", query = "SELECT u FROM AuditableUser u WHERE u.firstname = ?1")
public class AuditableUser extends AbstractAuditable<AuditableUser, Integer> {

	private String firstname;

	@ManyToMany(
			cascade = { CascadeType.PERSIST, CascadeType.MERGE }) private final Set<AuditableRole> roles = new HashSet<>();

	public AuditableUser() {
		this(null);
	}

	public AuditableUser(@Nullable Integer id) {
		this(id, null);
	}

	public AuditableUser(@Nullable Integer id, String firstname) {
		setId(id);
		this.firstname = firstname;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(final String firstname) {
		this.firstname = firstname;
	}

	public void addRole(AuditableRole role) {
		this.roles.add(role);
	}

	public Set<AuditableRole> getRoles() {
		return roles;
	}
}
