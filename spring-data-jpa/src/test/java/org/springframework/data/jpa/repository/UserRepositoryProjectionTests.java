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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Address;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.sample.UserRepository.IdOnly;
import org.springframework.data.jpa.repository.sample.UserRepository.NameOnly;
import org.springframework.data.jpa.repository.sample.UserRepository.RolesAndFirstname;
import org.springframework.data.jpa.repository.sample.UserRepository.UserExcerpt;
import org.springframework.data.jpa.repository.sample.UserRepository.UserRoleCountDtoProjection;
import org.springframework.data.jpa.repository.sample.UserRepository.UserRoleCountInterfaceProjection;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for executing projecting query methods.
 *
 * @author Oliver Gierke
 * @author Krzysztof Krason
 * @author Greg Turnquist
 * @author Mark Paluch
 * @author Christoph Strobl
 * @see QueryLookupStrategy
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context-h2.xml")
@Transactional
class UserRepositoryProjectionTests {

	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;
	@Autowired EntityManager em;

	PersistenceProvider provider;

	private User dave;
	private User carter;
	private User oliver;
	private Role drummer;
	private Role guitarist;
	private Role singer;

	@BeforeEach
	void setUp() {

		drummer = roleRepository.save(new Role("DRUMMER"));
		guitarist = roleRepository.save(new Role("GUITARIST"));
		singer = roleRepository.save(new Role("SINGER"));

		dave = userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", singer));
		carter = userRepository.save(new User("Carter", "Beauford", "carter@dmband.com", singer, drummer));
		oliver = userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));

		provider = PersistenceProvider.fromEntityManager(em);
	}

	@AfterEach
	void clearUp() {

		userRepository.deleteAll();
		roleRepository.deleteAll();
	}

	@Test // DATAJPA-974, GH-2815
	void executesQueryWithProjectionContainingReferenceToPluralAttribute() {

		List<RolesAndFirstname> rolesAndFirstnameBy = userRepository.findRolesAndFirstnameBy();

		assertThat(rolesAndFirstnameBy).isNotNull();

		for (RolesAndFirstname rolesAndFirstname : rolesAndFirstnameBy) {
			assertThat(rolesAndFirstname.getFirstname()).isNotNull();
			assertThat(rolesAndFirstname.getRoles()).isNotNull();
		}
	}

	@Test // GH-2815
	void executesQueryWithProjectionThroughStringQuery() {

		List<IdOnly> ids = userRepository.findIdOnly();

		assertThat(ids).isNotNull();

		assertThat(ids).extracting(IdOnly::getId).doesNotContainNull();
	}

	@Test // DATAJPA-1334
	void executesNamedQueryWithConstructorExpression() {
		userRepository.findByNamedQueryWithConstructorExpression();
	}

	@Test // DATAJPA-1713, GH-2008
	void selectProjectionWithSubselect() {

		List<NameOnly> dtos = userRepository.findProjectionBySubselect();

		assertThat(dtos).flatExtracting(NameOnly::getFirstname) //
				.containsExactly("Dave", "Carter", "Oliver August");
		assertThat(dtos).flatExtracting(NameOnly::getLastname) //
				.containsExactly("Matthews", "Beauford", "Matthews");
	}

	@Test // GH-3076
	void dtoProjectionShouldApplyConstructorExpressionRewriting() {

		List<UserExcerpt> dtos = userRepository.findRecordProjection();

		assertThat(dtos).flatExtracting(UserExcerpt::firstname) //
				.contains("Dave", "Carter", "Oliver August");

		dtos = userRepository.findRecordProjectionWithFunctions();

		assertThat(dtos).flatExtracting(UserExcerpt::lastname) //
				.contains("matthews", "beauford");
	}

	@Test // GH-3895
	void stringProjectionShouldNotApplyConstructorExpressionRewriting() {

		List<String> names = userRepository.findStringProjection();

		assertThat(names) //
				.contains("Dave", "Carter", "Oliver August");
	}

	@Test // GH-3895
	void objectArrayProjectionShouldNotApplyConstructorExpressionRewriting() {

		List<Object[]> names = userRepository.findObjectArrayProjectionWithFunctions();

		assertThat(names) //
				.contains(new String[] { "Dave", "matthews" });
	}

	@Test // GH-3076
	void dtoMultiselectProjectionShouldApplyConstructorExpressionRewriting() {

		List<UserExcerpt> dtos = userRepository.findMultiselectRecordProjection();

		assertThat(dtos).flatExtracting(UserExcerpt::firstname) //
				.contains("Dave", "Carter", "Oliver August");
	}

	@Test // GH-3895
	void dtoMultiselectProjectionShouldApplyConstructorExpressionRewritingForJoin() {

		dave.setAddress(new Address("US", "Albuquerque", "some street", "12345"));

		List<UserRepository.AddressDto> dtos = userRepository.findAddressProjection();

		assertThat(dtos).flatExtracting(UserRepository.AddressDto::city) //
				.contains("Albuquerque");
	}

	@Test // GH-3076
	void dynamicDtoProjection() {

		List<UserExcerpt> dtos = userRepository.findRecordProjection(UserExcerpt.class);

		assertThat(dtos).flatExtracting(UserExcerpt::firstname) //
				.contains("Dave", "Carter", "Oliver August");
	}

	@Test // GH-3862
	void shouldNotRewritePrimitiveSelectionToDtoProjection() {

		oliver.setAge(28);
		em.persist(oliver);

		assertThat(userRepository.findAgeByAnnotatedQuery(oliver.getEmailAddress())).contains(28);
	}

	@Test // GH-3862
	void shouldNotRewritePropertySelectionToDtoProjection() {

		Address address = new Address("DE", "Dresden", "some street", "12345");
		dave.setAddress(address);
		userRepository.save(dave);
		em.flush();
		em.clear();

		assertThat(userRepository.findAddressByAnnotatedQuery(dave.getEmailAddress())).contains(address);
		assertThat(userRepository.findCityByAnnotatedQuery(dave.getEmailAddress())).contains("Dresden");
		assertThat(userRepository.findRolesByAnnotatedQuery(dave.getEmailAddress())).contains(singer);
	}

	@Test // GH-3076
	void dtoProjectionWithEntityAndAggregatedValue() {

		Map<String, User> musicians = Map.of(carter.getFirstname(), carter, dave.getFirstname(), dave,
				oliver.getFirstname(), oliver);

		assertThat(userRepository.dtoProjectionEntityAndAggregatedValue()).allSatisfy(projection -> {
			assertThat(projection.user()).isIn(musicians.values());
			assertThat(projection.roleCount()).isCloseTo(musicians.get(projection.user().getFirstname()).getRoles().size(),
					Offset.offset(0L));
		});
	}

	@Test // GH-3076
	void interfaceProjectionWithEntityAndAggregatedValue() {

		Map<String, User> musicians = Map.of(carter.getFirstname(), carter, dave.getFirstname(), dave,
				oliver.getFirstname(), oliver);

		assertThat(userRepository.interfaceProjectionEntityAndAggregatedValue()).allSatisfy(projection -> {
			assertThat(projection.getUser()).isIn(musicians.values());
			assertThat(projection.getRoleCount())
					.isCloseTo(musicians.get(projection.getUser().getFirstname()).getRoles().size(), Offset.offset(0L));
		});
	}

	@Test // GH-3076
	void rawMapProjectionWithEntityAndAggregatedValue() {

		Map<String, User> musicians = Map.of(carter.getFirstname(), carter, dave.getFirstname(), dave,
				oliver.getFirstname(), oliver);

		assertThat(userRepository.rawMapProjectionEntityAndAggregatedValue()).allSatisfy(projection -> {
			assertThat(projection.get("user")).isIn(musicians.values());
			assertThat(projection).containsKey("roleCount");
		});
	}

	@Test // GH-3076
	void dtoProjectionWithEntityAndAggregatedValueWithPageable() {

		Map<String, User> musicians = Map.of(carter.getFirstname(), carter, dave.getFirstname(), dave,
				oliver.getFirstname(), oliver);

		assertThat(
				userRepository.dtoProjectionEntityAndAggregatedValue(PageRequest.of(0, 10).withSort(Sort.by("firstname"))))
				.allSatisfy(projection -> {
					assertThat(projection.user()).isIn(musicians.values());
					assertThat(projection.roleCount())
							.isCloseTo(musicians.get(projection.user().getFirstname()).getRoles().size(), Offset.offset(0L));
				});
	}

	@ParameterizedTest // GH-3076
	@ValueSource(classes = { UserRoleCountDtoProjection.class, UserRoleCountInterfaceProjection.class })
	<T> void dynamicProjectionWithEntityAndAggregated(Class<T> resultType) {

		assertThat(userRepository.findMultiselectRecordDynamicProjection(resultType)).hasSize(3)
				.hasOnlyElementsOfType(resultType);
	}

}
