/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.projections;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Reda.Housni-Alaoui
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ProjectionsIntegrationTests.Config.class)
class ProjectionJoinIntegrationTests {

	@Autowired private UserRepository userRepository;

	@Test // DATAJPA-1418
	void findByIdPerformsAnOuterJoin() {
		User user = userRepository.save(new User());

		UserProjection projection = userRepository.findById(user.getId(), UserProjection.class);

		assertThat(projection).isNotNull();
		assertThat(projection.getId()).isEqualTo(user.getId());
		assertThat(projection.getAddress()).isNull();
	}

	public static class UserProjection {

		private final int id;
		private final Address address;

		public UserProjection(int id, Address address) {
			this.id = id;
			this.address = address;
		}

		public int getId() {
			return this.id;
		}

		public Address getAddress() {
			return this.address;
		}

		public String toString() {
			return "ProjectionJoinIntegrationTests.UserProjection(id=" + this.getId() + ", address=" + this.getAddress()
					+ ")";
		}
	}

	public interface UserRepository extends CrudRepository<User, Integer> {

		<T> T findById(int id, Class<T> projectionClass);
	}

	@Table(name = "ProjectionJoinIntegrationTests_User")
	@Entity
	static class User {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Access(value = AccessType.PROPERTY) int id;

		@OneToOne(cascade = CascadeType.ALL) Address address;

		public User() {}

		public int getId() {
			return this.id;
		}

		public Address getAddress() {
			return this.address;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public String toString() {
			return "ProjectionJoinIntegrationTests.User(id=" + this.getId() + ", address=" + this.getAddress() + ")";
		}
	}

	@Table(name = "ProjectionJoinIntegrationTests_Address")
	@Entity
	static class Address {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		@Access(value = AccessType.PROPERTY) //
		int id;

		String streetName;

		public Address() {}

		public int getId() {
			return this.id;
		}

		public String getStreetName() {
			return this.streetName;
		}

		public void setId(int id) {
			this.id = id;
		}

		public void setStreetName(String streetName) {
			this.streetName = streetName;
		}

		public String toString() {
			return "ProjectionJoinIntegrationTests.Address(id=" + this.getId() + ", streetName=" + this.getStreetName() + ")";
		}
	}
}
