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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Sample domain class representing roles. Mapped with XML.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Entity
public class Role {

	private static final String PREFIX = "ROLE_";

	@Id @GeneratedValue private Integer id;
	private String name;

	/**
	 * Creates a new instance of {@code Role}.
	 */
	public Role() {}

	/**
	 * Creates a new preconfigured {@code Role}.
	 *
	 * @param name
	 */
	public Role(final String name) {
		this.name = name;
	}

	/**
	 * Returns the id.
	 *
	 * @return
	 */
	public Integer getId() {

		return id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return PREFIX + name;
	}

	/**
	 * Returns whether the role is to be considered new.
	 *
	 * @return
	 */
	public boolean isNew() {

		return id == null;
	}
}
