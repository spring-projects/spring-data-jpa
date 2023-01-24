/*
 * Copyright 2013-2023 the original author or authors.
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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolver;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.sample.Category;
import org.springframework.data.jpa.domain.sample.Invoice;
import org.springframework.data.jpa.domain.sample.InvoiceItem;
import org.springframework.data.jpa.domain.sample.Order;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.infrastructure.HibernateTestUtils;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link QueryUtils}.
 *
 * @author Oliver Gierke
 * @author Sébastien Péralta
 * @author Jens Schauder
 * @author Patrice Blanchardie
 * @author Diego Krupitza
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class QueryUtilsIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test // DATAJPA-403
	void reusesExistingJoinForExpression() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> from = query.from(User.class);

		PropertyPath managerFirstname = PropertyPath.from("manager.firstname", User.class);
		PropertyPath managerLastname = PropertyPath.from("manager.lastname", User.class);

		QueryUtils.toExpressionRecursively(from, managerLastname);
		QueryUtils.toExpressionRecursively(from, managerFirstname);

		assertThat(from.getJoins()).hasSize(1);
	}

	@Test // DATAJPA-401, DATAJPA-1238
	void createsJoinForNavigationAcrossOptionalAssociation() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("manager.firstname", User.class));

		assertThat(getNonInnerJoins(root)).hasSize(1);
	}

	@Test // DATAJPA-1404
	void createsJoinForOptionalOneToOneInReverseDirection() {

		doInMerchantContext(emf -> {

			CriteriaBuilder builder = emf.getCriteriaBuilder();
			CriteriaQuery<Address> query = builder.createQuery(Address.class);
			Root<Address> root = query.from(Address.class);

			QueryUtils.toExpressionRecursively(root, PropertyPath.from("merchant", Address.class));

			assertThat(getNonInnerJoins(root)).hasSize(1);
		});
	}

	@Test // gh-2111
	void createsLeftJoinForOptionalToOneWithNestedNonOptional() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Invoice> query = builder.createQuery(Invoice.class);
		Root<Invoice> root = query.from(Invoice.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("order.customer.name", Invoice.class), false);

		assertThat(getNonInnerJoins(root)).hasSize(1); // left join order
		assertThat(getInnerJoins(root)).isEmpty(); // no inner join order
		Join<Invoice, ?> leftJoin = root.getJoins().iterator().next();
		assertThat(getNonInnerJoins(leftJoin)).hasSize(1); // left join customer
		assertThat(getInnerJoins(leftJoin)).isEmpty(); // no inner join customer
	}

	@Test // gh-2111
	void createsLeftJoinForNonOptionalToOneWithNestedOptional() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<InvoiceItem> query = builder.createQuery(InvoiceItem.class);
		Root<InvoiceItem> root = query.from(InvoiceItem.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("invoice.order.customer.name", InvoiceItem.class),
				false);

		assertThat(getInnerJoins(root)).hasSize(1); // join invoice
		Join<?, ?> rootInnerJoin = getInnerJoins(root).iterator().next();
		assertThat(getNonInnerJoins(rootInnerJoin)).hasSize(1); // left join order
		assertThat(getInnerJoins(rootInnerJoin)).isEmpty(); // no inner join order
		Join<?, ?> leftJoin = getNonInnerJoins(rootInnerJoin).iterator().next();
		assertThat(getNonInnerJoins(leftJoin)).hasSize(1); // left join customer
		assertThat(getInnerJoins(leftJoin)).isEmpty(); // no inner join customer
	}

	@Test // gh-2111
	void reusesLeftJoinForNonOptionalToOneWithNestedOptional() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<InvoiceItem> query = builder.createQuery(InvoiceItem.class);
		Root<InvoiceItem> root = query.from(InvoiceItem.class);

		// given existing left joins
		root.join("invoice", JoinType.LEFT).join("order", JoinType.LEFT);

		// when navigating through a path with nested optionals
		QueryUtils.toExpressionRecursively(root, PropertyPath.from("invoice.order.customer.name", InvoiceItem.class),
				false);

		// assert that existing joins are reused and no additional joins are created
		assertThat(getInnerJoins(root)).isEmpty(); // no inner join invoice
		assertThat(getNonInnerJoins(root)).hasSize(1); // reused left join invoice
		Join<?, ?> rootInnerJoin = getNonInnerJoins(root).iterator().next();
		assertThat(getInnerJoins(rootInnerJoin)).isEmpty(); // no inner join order
		assertThat(getNonInnerJoins(rootInnerJoin)).hasSize(1); // reused left join order
		Join<?, ?> leftJoin = getNonInnerJoins(rootInnerJoin).iterator().next();
		assertThat(getNonInnerJoins(leftJoin)).hasSize(1); // left join customer
	}

	@Test // gh-2111
	void reusesInnerJoinForNonOptionalToOneWithNestedOptional() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<InvoiceItem> query = builder.createQuery(InvoiceItem.class);
		Root<InvoiceItem> root = query.from(InvoiceItem.class);

		// given an existing inner join a nested optional
		root.join("invoice").join("order");

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("invoice.order.customer.name", InvoiceItem.class),
				false);

		// assert that no useless left joins are created
		assertThat(getInnerJoins(root)).hasSize(1); // join invoice
		Join<?, ?> rootInnerJoin = getInnerJoins(root).iterator().next();
		assertThat(getNonInnerJoins(rootInnerJoin)).isEmpty(); // no left join order
		assertThat(getInnerJoins(rootInnerJoin)).hasSize(1); // inner join order
		Join<?, ?> innerJoin = getInnerJoins(rootInnerJoin).iterator().next();
		assertThat(getNonInnerJoins(innerJoin)).isEmpty(); // no left join customer
		assertThat(getInnerJoins(innerJoin)).hasSize(1); // inner join customer
	}

	@Test // DATAJPA-1404
	void createsNoJoinForOptionalOneToOneInNormalDirection() {

		doInMerchantContext(emf -> {

			CriteriaBuilder builder = emf.getCriteriaBuilder();
			CriteriaQuery<Merchant> query = builder.createQuery(Merchant.class);
			Root<Merchant> root = query.from(Merchant.class);

			QueryUtils.toExpressionRecursively(root, PropertyPath.from("address", Merchant.class));

			assertThat(getNonInnerJoins(root)).isEmpty();
		});
	}

	@Test // DATAJPA-401, DATAJPA-1238
	void doesNotCreateJoinForOptionalAssociationWithoutFurtherNavigation() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("manager", User.class));

		assertThat(getNonInnerJoins(root)).isEmpty();
	}

	@Test // DATAJPA-401
	void doesNotCreateAJoinForNonOptionalAssociation() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Order> query = builder.createQuery(Order.class);
		Root<Order> root = query.from(Order.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("customer", Order.class));
	}

	@Test // DATAJPA-454
	void createsJoinToTraverseCollectionPath() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);

		QueryUtils.toExpressionRecursively(root, PropertyPath.from("colleaguesLastname", User.class));

		assertThat(root.getJoins()).hasSize(1);
	}

	@Test // DATAJPA-476
	void traversesPluralAttributeCorrectly() {

		doInMerchantContext((emf) -> {

			CriteriaBuilder builder = emf.createEntityManager().getCriteriaBuilder();
			CriteriaQuery<Merchant> query = builder.createQuery(Merchant.class);
			Root<Merchant> root = query.from(Merchant.class);

			QueryUtils.toExpressionRecursively(root, PropertyPath.from("employeesCredentialsUid", Merchant.class));
		});
	}

	void doInMerchantContext(Consumer<EntityManagerFactory> emfConsumer) {
		PersistenceProviderResolver originalPersistenceProviderResolver = PersistenceProviderResolverHolder
				.getPersistenceProviderResolver();

		EntityManagerFactory entityManagerFactory = null;
		try {

			PersistenceProviderResolverHolder.setPersistenceProviderResolver(new HibernateOnlyPersistenceProviderResolver());
			entityManagerFactory = Persistence.createEntityManagerFactory("merchant");
			emfConsumer.accept(entityManagerFactory);
		} finally {
			PersistenceProviderResolverHolder.setPersistenceProviderResolver(originalPersistenceProviderResolver);
			if (entityManagerFactory != null) {
				entityManagerFactory.close();
			}
		}
	}

	@Test // DATAJPA-763
	@SuppressWarnings("unchecked")
	void doesNotCreateAJoinForAlreadyFetchedAssociation() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Category> query = builder.createQuery(Category.class);

		Root<Category> root = query.from(Category.class);

		Root<Category> mock = Mockito.mock(Root.class);
		doReturn(root.getModel()).when(mock).getModel();
		doReturn(Collections.singleton(root.fetch("product", JoinType.LEFT))).when(mock).getFetches();

		QueryUtils.toExpressionRecursively(mock, PropertyPath.from("product", Category.class));

		verify(mock, times(1)).get("product");
		verify(mock, times(0)).join(Mockito.eq("product"), Mockito.any(JoinType.class));
	}

	@Test // DATAJPA-1080
	void toOrdersCanSortByJoinColumn() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);
		Join<User, User> join = root.join("manager", JoinType.LEFT);

		Sort sort = Sort.by(Direction.ASC, "age");

		List<jakarta.persistence.criteria.Order> orders = QueryUtils.toOrders(sort, join, builder);

		assertThat(orders).hasSize(1);
	}

	/**
	 * This test documents an ambiguity in the JPA spec (or it's implementation in Hibernate vs EclipseLink) that we have
	 * to work around in the test {@link #doesNotCreateJoinForOptionalAssociationWithoutFurtherNavigation()}. See also:
	 * https://github.com/javaee/jpa-spec/issues/169 Compare to: {@link EclipseLinkQueryUtilsIntegrationTests}
	 */
	@Test // DATAJPA-1238
	void demonstrateDifferentBehaviorOfGetJoin() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> root = query.from(User.class);

		root.get("manager");

		assertThat(root.getJoins()).hasSize(getNumberOfJoinsAfterCreatingAPath());
	}

	int getNumberOfJoinsAfterCreatingAPath() {
		return 0;
	}

	private Set<Join<?, ?>> getNonInnerJoins(From<?, ?> root) {

		return root.getJoins().stream().filter(j -> j.getJoinType() != JoinType.INNER).collect(Collectors.toSet());
	}

	private Set<Join<?, ?>> getInnerJoins(From<?, ?> root) {

		return root.getJoins().stream().filter(j -> j.getJoinType() == JoinType.INNER).collect(Collectors.toSet());
	}

	@Entity
	@SuppressWarnings("unused")
	static class Merchant {

		@Id String id;
		@OneToMany Set<Employee> employees;

		@OneToOne Address address;
	}

	@Entity
	@SuppressWarnings("unused")
	static class Address {
		@Id String id;
		@OneToOne(mappedBy = "address") Merchant merchant;
	}

	@Entity
	@SuppressWarnings("unused")
	static class Employee {

		@Id String id;
		@OneToMany Set<Credential> credentials;
	}

	@Entity
	@SuppressWarnings("unused")
	static class Credential {

		@Id String id;
		String uid;
	}

	/**
	 * A {@link PersistenceProviderResolver} that returns only a Hibernate {@link PersistenceProvider} and ignores others.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 */
	static class HibernateOnlyPersistenceProviderResolver implements PersistenceProviderResolver {

		@Override
		public List<PersistenceProvider> getPersistenceProviders() {
			return singletonList(HibernateTestUtils.getPersistenceProvider());
		}

		@Override
		public void clearCachedProviders() {}
	}

}
