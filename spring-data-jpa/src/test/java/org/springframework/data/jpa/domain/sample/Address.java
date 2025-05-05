/*
 * Copyright 2013-2025 the original author or authors.
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

import jakarta.persistence.Embeddable;

import org.springframework.util.ObjectUtils;

/**
 * @author Thomas Darimont
 */
@Embeddable
public class Address {

	private String country;
	private String city;
	private String streetName;
	private String streetNo;

	public Address() {}

	public Address(String country, String city, String streetName, String streetNo) {
		this.country = country;
		this.city = city;
		this.streetName = streetName;
		this.streetNo = streetNo;
	}

	public String getCountry() {
		return country;
	}

	public String getCity() {
		return city;
	}

	public String getStreetName() {
		return streetName;
	}

	public String getStreetNo() {
		return streetNo;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Address address)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(country, address.country)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(city, address.city)) {
			return false;
		}
		if (!ObjectUtils.nullSafeEquals(streetName, address.streetName)) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(streetNo, address.streetNo);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(country, city, streetName, streetNo);
	}
}
