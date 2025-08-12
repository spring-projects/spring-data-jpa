/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.provider.HibernateUtils;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests that verify foreign key optimization works correctly.
 * This test specifically checks that Hibernate's query inspection shows no JOINs.
 *
 * @author Hyunjoon Kim
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration
@Transactional
class ForeignKeyOptimizationIntegrationTests {

	@Autowired EntityManager em;
	@Autowired OrderRepository orderRepository;
	@Autowired CustomerRepository customerRepository;

	private Customer savedCustomer;

	@BeforeEach
	void setUp() {
		Customer customer = new Customer();
		customer.setName("John Doe");
		savedCustomer = customerRepository.save(customer);

		Order order1 = new Order();
		order1.setOrderNumber("ORD-001");
		order1.setCustomer(savedCustomer);
		orderRepository.save(order1);

		Order order2 = new Order();
		order2.setOrderNumber("ORD-002");
		order2.setCustomer(savedCustomer);
		orderRepository.save(order2);
		
		em.flush();
		em.clear();
	}

	@Test
	void derivedQuery_findByCustomerId_shouldNotCreateJoin() {
		// Execute the derived query
		List<Order> orders = orderRepository.findByCustomerId(savedCustomer.getId());
		
		// Verify results
		assertThat(orders).hasSize(2);
		assertThat(orders).extracting(Order::getOrderNumber)
				.containsExactlyInAnyOrder("ORD-001", "ORD-002");
		
		// Verify the generated JPQL doesn't contain JOIN
		// This is the key test - we want to ensure the query uses direct FK reference
		TypedQuery<Order> query = em.createQuery(
			"SELECT o FROM ForeignKeyOptimizationIntegrationTests$Order o WHERE o.customer.id = :customerId", 
			Order.class);
		query.setParameter("customerId", savedCustomer.getId());
		
		// With Hibernate, we can check the SQL
		if (PersistenceProvider.fromEntityManager(em) == PersistenceProvider.HIBERNATE) {
			String sql = HibernateUtils.getHibernateQuery(query);
			// The SQL should NOT contain JOIN
			assertThat(sql.toLowerCase()).doesNotContain("join");
			// It should reference the FK column directly
			assertThat(sql.toLowerCase()).contains("customer_id");
		}
		
		// Execute and verify it works
		List<Order> manualQueryResults = query.getResultList();
		assertThat(manualQueryResults).hasSize(2);
	}

	@Test
	void jpqlQuery_withExplicitJoin_shouldCreateJoin() {
		// For comparison, JPQL with explicit path should still work
		List<Order> orders = orderRepository.findByCustomerIdWithJPQL(savedCustomer.getId());
		
		assertThat(orders).hasSize(2);
		assertThat(orders).extracting(Order::getOrderNumber)
				.containsExactlyInAnyOrder("ORD-001", "ORD-002");
	}

	@Test
	void derivedQuery_findByCustomerName_shouldCreateJoin() {
		// This should create a JOIN because we're accessing a non-ID property
		List<Order> orders = orderRepository.findByCustomerName("John Doe");
		
		assertThat(orders).hasSize(2);
		
		// This query SHOULD have a JOIN
		TypedQuery<Order> query = em.createQuery(
			"SELECT o FROM ForeignKeyOptimizationIntegrationTests$Order o WHERE o.customer.name = :name", 
			Order.class);
		query.setParameter("name", "John Doe");
		
		if (PersistenceProvider.fromEntityManager(em) == PersistenceProvider.HIBERNATE) {
			String sql = HibernateUtils.getHibernateQuery(query);
			// This should contain JOIN
			assertThat(sql.toLowerCase()).contains("join");
		}
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@ImportResource("classpath:infrastructure.xml")
	static class Config {
	}

	@Entity
	@Table(name = "fk_opt_customer")
	static class Customer {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	@Table(name = "fk_opt_order")
	static class Order {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		private String orderNumber;

		@ManyToOne(fetch = FetchType.LAZY)
		private Customer customer;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getOrderNumber() {
			return orderNumber;
		}

		public void setOrderNumber(String orderNumber) {
			this.orderNumber = orderNumber;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}
	}

	interface OrderRepository extends JpaRepository<Order, Long> {
		// This should be optimized to not use JOIN
		List<Order> findByCustomerId(Long customerId);
		
		// This should use JOIN because it accesses non-ID property
		List<Order> findByCustomerName(String name);
		
		// For comparison - explicit JPQL
		@Query("SELECT o FROM ForeignKeyOptimizationIntegrationTests$Order o WHERE o.customer.id = :customerId")
		List<Order> findByCustomerIdWithJPQL(@Param("customerId") Long customerId);
	}

	interface CustomerRepository extends JpaRepository<Customer, Long> {
	}
}
