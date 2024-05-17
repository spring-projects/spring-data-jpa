/*
 * Copyright 2024 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jmh.mbr.junit5.Microbenchmark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.springframework.data.jpa.model.IPersonProjection;
import org.springframework.data.jpa.model.Person;
import org.springframework.data.jpa.model.Profile;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.util.ObjectUtils;

/**
 * @author Christoph Strobl
 */
@Microbenchmark
@Warmup(time = 5, iterations = 3)
@Measurement(time = 5)
public class RepositoryFinderTests {

	@State(Scope.Benchmark)
	public static class BenchmarkParameters {

		EntityManager entityManager;
		PersonRepository repositoryProxy;

		@Setup(Level.Iteration)
		public void doSetup() {

			createEntityManager();

			if (!entityManager.getTransaction().isActive()) {

				if (ObjectUtils.nullSafeEquals(
						entityManager.createNativeQuery("SELECT COUNT(*) FROM person", Integer.class).getSingleResult(),
						Integer.valueOf(0))) {

					entityManager.getTransaction().begin();

					Profile generalProfile = new Profile("general");
					Profile sdUserProfile = new Profile("sd-user");

					entityManager.persist(generalProfile);
					entityManager.persist(sdUserProfile);

					Person person = new Person("first", "last");
					person.setProfiles(Set.of(generalProfile, sdUserProfile));
					entityManager.persist(person);
					entityManager.getTransaction().commit();
				}
			}

			this.repositoryProxy = createRepository();
		}

		@TearDown(Level.Iteration)
		public void doTearDown() {
			entityManager.close();
		}

		private void createEntityManager() {

			Map<String, String> properties = new HashMap<>();
			properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test");
			properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			properties.put("hibernate.hbm2ddl.auto", "update");
			EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("benchmark", properties);

			entityManager = entityManagerFactory.createEntityManager();
		}

		PersonRepository createRepository() {
			JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);
			return repositoryFactory.getRepository(PersonRepository.class);
		}
	}

	@Benchmark
	public PersonRepository repositoryBootstrap(BenchmarkParameters parameters) {
		return parameters.createRepository();
	}

	@Benchmark
	public List<Person> baselineEntityManagerCriteriaQuery(BenchmarkParameters parameters) {

		CriteriaBuilder criteriaBuilder = parameters.entityManager.getCriteriaBuilder();
		CriteriaQuery<Person> query = criteriaBuilder.createQuery(Person.class);
		Root<Person> root = query.from(Person.class);
		TypedQuery<Person> typedQuery = parameters.entityManager
				.createQuery(query.where(criteriaBuilder.equal(root.get("firstname"), "first")));

		return typedQuery.getResultList();
	}

	@Benchmark
	public List<Person> baselineEntityManagerHQLQuery(BenchmarkParameters parameters) {

		Query query = parameters.entityManager
				.createQuery("SELECT p FROM org.springframework.data.jpa.model.Person p WHERE p.firstname = ?1");
		query.setParameter(1, "first");

		return query.getResultList();
	}

	@Benchmark
	public List<Person> derivedFinderMethod(BenchmarkParameters parameters) {
		return parameters.repositoryProxy.findAllByFirstname("first");
	}

	@Benchmark
	public List<IPersonProjection> derivedFinderMethodWithInterfaceProjection(BenchmarkParameters parameters) {
		return parameters.repositoryProxy.findAllAndProjectToInterfaceByFirstname("first");
	}

	@Benchmark
	public List<Person> stringBasedQuery(BenchmarkParameters parameters) {
		return parameters.repositoryProxy.findAllWithAnnotatedQueryByFirstname("first");
	}

	@Benchmark
	public List<Person> stringBasedNativeQuery(BenchmarkParameters parameters) {
		return parameters.repositoryProxy.findAllWithNativeQueryByFirstname("first");
	}

}
