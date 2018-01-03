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

import javax.persistence.Entity;

import org.springframework.data.jpa.domain.AbstractAuditable;

/**
 * Sample auditable role entity.
 *
 * @author Oliver Gierke
 */
@Entity
public class AuditableRole extends AbstractAuditable<AuditableUser, Long> {

	private static final long serialVersionUID = 5997359055260303863L;

	private String name;

	public void setName(String name) {

		this.name = name;
	}

	public String getName() {

		return name;
	}
}
