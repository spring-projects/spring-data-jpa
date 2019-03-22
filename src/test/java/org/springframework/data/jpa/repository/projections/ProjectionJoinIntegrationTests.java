/*
 * Copyright 2018-2019 the original author or authors.
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

import lombok.Data;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Reda.Housni-Alaoui
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ProjectionsIntegrationTests.Config.class)
public class ProjectionJoinIntegrationTests {

	@Autowired private UserRepository userRepository;

	@Test // DATAJPA-1418
	public void findByIdPerformsAnOuterJoin() {
		User user = userRepository.save(new User());

		UserProjection projection = userRepository.findById(user.getId(), UserProjection.class);

		assertThat(projection).isNotNull();
		assertThat(projection.getId()).isEqualTo(user.getId());
		assertThat(projection.getAddress()).isNull();
	}

	@Data
	private static class UserProjection {

		private final int id;
		private final Address address;

		public UserProjection(int id, Address address) {
			this.id = id;
			this.address = address;
		}
	}

	public interface UserRepository extends CrudRepository<User, Integer> {

		<T> T findById(int id, Class<T> projectionClass);
	}

	@Data
	@Table(name = "ProjectionJoinIntegrationTests_User")
	@Entity
	static class User {
		@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Access(value = AccessType.PROPERTY) int id;

		@OneToOne(cascade = CascadeType.ALL) Address address;
	}

	@Data
	@Table(name = "ProjectionJoinIntegrationTests_Address")
	@Entity
	static class Address {
		@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Access(value = AccessType.PROPERTY) int id;

		String streetName;
	}
}
