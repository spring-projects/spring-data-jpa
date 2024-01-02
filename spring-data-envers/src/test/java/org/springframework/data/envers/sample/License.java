/*
 * Copyright 2012-2024 the original author or authors.
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
package org.springframework.data.envers.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;

import java.util.Objects;
import java.util.Set;

import org.hibernate.envers.Audited;

/**
 * Sample domain class.
 *
 * @author Philip Huegelmeyer
 */
@Audited
@Entity
public class License extends AbstractEntity {

	@Version public Integer version;

	public String name;
	@ManyToMany public Set<Country> laender;

	@Override
	public boolean equals(Object o) {

		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		License license = (License) o;
		return Objects.equals(version, license.version) && Objects.equals(name, license.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, name);
	}
}
