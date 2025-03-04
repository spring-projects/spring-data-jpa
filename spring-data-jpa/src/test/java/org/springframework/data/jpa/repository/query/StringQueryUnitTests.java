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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.query.ParameterBinding.BindingIdentifier;
import org.springframework.data.jpa.repository.query.ParameterBinding.Expression;
import org.springframework.data.jpa.repository.query.ParameterBinding.InParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.ParameterBinding.MethodInvocationArgument;
import org.springframework.data.jpa.repository.query.ParameterBinding.ParameterOrigin;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBindingParser;
import org.springframework.data.repository.query.parser.Part.Type;

/**
 * Unit tests for {@link StringQuery}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Nils Borrmann
 * @author Andriy Redko
 * @author Diego Krupitza
 * @author Mark Paluch
 * @author Aleksei Elin
 */
class StringQueryUnitTests {

	@Test // DATAJPA-341
	void doesNotConsiderPlainLikeABinding() {

		String source = "select u from User u where u.firstname like :firstname";
		StringQuery query = new StringQuery(source, false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(source);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding.getType()).isEqualTo(Type.LIKE);

		assertThat(binding.getName()).isEqualTo("firstname");
	}

	@Test // DATAJPA-292
	void detectsPositionalLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %?1% or u.lastname like %?2",
				true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString())
				.isEqualTo("select u from User u where u.firstname like ?1 or u.lastname like ?2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getRequiredPosition()).isEqualTo(1);
		assertThat(binding.getType()).isEqualTo(Type.CONTAINING);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getRequiredPosition()).isEqualTo(2);
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);
	}

	@Test // DATAJPA-292, GH-3041
	void detectsAnonymousLikeBindings() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like %?% or u.lastname like %? or u.lastname=?", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString())
				.isEqualTo("select u from User u where u.firstname like ? or u.lastname like ? or u.lastname=?");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin()).isEqualTo(ParameterOrigin.ofParameter(1));
		assertThat(binding.getRequiredPosition()).isEqualTo(1);
		assertThat(binding.getType()).isEqualTo(Type.CONTAINING);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin()).isEqualTo(ParameterOrigin.ofParameter(2));
		assertThat(binding.getRequiredPosition()).isEqualTo(2);
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);
	}

	@Test // DATAJPA-292, GH-3041
	void detectsNamedLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %:firstname", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo("select u from User u where u.firstname like :firstname");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getRequiredName()).isEqualTo("firstname");
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);
	}

	@Test // GH-3041
	void rewritesNamedLikeToUniqueParametersIfNecessary() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like %:firstname or u.firstname like :firstname% or u.firstname = :firstname",
				true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()) //
				.isEqualTo(
						"select u from User u where u.firstname like :firstname or u.firstname like :firstname_1 or u.firstname = :firstname_2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getName()).isEqualTo("firstname");
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getName()).isEqualTo("firstname_1");
		assertThat(binding.getType()).isEqualTo(Type.STARTING_WITH);

		ParameterBinding parameterBinding = bindings.get(2);
		assertThat(parameterBinding).isNotNull();
		assertThat(parameterBinding.getName()).isEqualTo("firstname_2");
		assertThat(((MethodInvocationArgument) parameterBinding.getOrigin()).identifier().getName()).isEqualTo("firstname");
	}

	@Test // GH-3784
	void rewritesNamedLikeToUniqueParametersRetainingCountQuery() {

		DeclaredQuery query = new StringQuery(
				"select u from User u where u.firstname like %:firstname or u.firstname like :firstname% or u.firstname = :firstname",
				false).deriveCountQuery(null);

		assertThat(query.getQueryString()) //
				.isEqualTo(
						"select count(u) from User u where u.firstname like :firstname or u.firstname like :firstname_1 or u.firstname = :firstname_2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin()).isEqualTo(ParameterOrigin.ofParameter("firstname"));
		assertThat(binding.getName()).isEqualTo("firstname");
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin()).isEqualTo(ParameterOrigin.ofParameter("firstname"));
		assertThat(binding.getName()).isEqualTo("firstname_1");
		assertThat(binding.getType()).isEqualTo(Type.STARTING_WITH);

		ParameterBinding parameterBinding = bindings.get(2);
		assertThat(parameterBinding).isNotNull();
		assertThat(parameterBinding.getOrigin()).isEqualTo(ParameterOrigin.ofParameter("firstname"));
		assertThat(parameterBinding.getName()).isEqualTo("firstname_2");
		assertThat(((MethodInvocationArgument) parameterBinding.getOrigin()).identifier().getName()).isEqualTo("firstname");
	}

	@Test // GH-3784
	void rewritesExpressionsLikeToUniqueParametersRetainingCountQuery() {

		DeclaredQuery query = new StringQuery(
				"select u from User u where u.firstname like %:#{firstname} or u.firstname like :#{firstname}%", false)
				.deriveCountQuery(null);

		assertThat(query.getQueryString()) //
				.isEqualTo(
						"select count(u) from User u where u.firstname like :__$synthetic$__1 or u.firstname like :__$synthetic$__2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin().isExpression()).isTrue();
		assertThat(binding.getName()).isEqualTo("__$synthetic$__1");
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.getOrigin().isExpression()).isTrue();
		assertThat(binding.getName()).isEqualTo("__$synthetic$__2");
		assertThat(binding.getType()).isEqualTo(Type.STARTING_WITH);
	}

	@Test // GH-3041
	void rewritesPositionalLikeToUniqueParametersIfNecessary() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like %?1 or u.firstname like ?1% or u.firstname = ?1", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString())
				.isEqualTo("select u from User u where u.firstname like ?1 or u.firstname like ?2 or u.firstname = ?3");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);
	}

	@Test // GH-3041
	void reusesNamedLikeBindingsWherePossible() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like %:firstname or u.firstname like %:firstname% or u.firstname like %:firstname% or u.firstname like %:firstname",
				true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where u.firstname like :firstname or u.firstname like :firstname_1 or u.firstname like :firstname_1 or u.firstname like :firstname");

		query = new StringQuery("select u from User u where u.firstname like %:firstname or u.firstname =:firstname", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString())
				.isEqualTo("select u from User u where u.firstname like :firstname or u.firstname =:firstname_1");
	}

	@Test // GH-3041
	void reusesPositionalLikeBindingsWherePossible() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like %?1 or u.firstname like %?1% or u.firstname like %?1% or u.firstname like %?1",
				false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where u.firstname like ?1 or u.firstname like ?2 or u.firstname like ?2 or u.firstname like ?1");

		query = new StringQuery("select u from User u where u.firstname like %?1 or u.firstname =?1", false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo("select u from User u where u.firstname like ?1 or u.firstname =?2");
	}

	@Test // GH-3041
	void shouldRewritePositionalBindingsWithParameterReuse() {

		StringQuery query = new StringQuery(
				"select u from User u where u.firstname like ?2 or u.firstname like %?2% or u.firstname like %?1% or u.firstname like %?1 OR u.firstname like ?1",
				false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where u.firstname like ?2 or u.firstname like ?3 or u.firstname like ?1 or u.firstname like ?4 OR u.firstname like ?5");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(5);

		assertThat(bindings).extracting(ParameterBinding::getRequiredPosition).containsOnly(1, 2, 3, 4, 5);
		assertThat(bindings).extracting(ParameterBinding::getOrigin) //
				.map(MethodInvocationArgument.class::cast) //
				.map(MethodInvocationArgument::identifier) //
				.map(BindingIdentifier::getPosition) //
				.containsOnly(1, 2);
	}

	@Test // GH-3758
	void createsDistinctBindingsForIndexedSpel() {

		StringQuery query = new StringQuery("select u from User u where u.firstname = ?#{foo} OR u.firstname = ?#{foo}",
				false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getParameterBindings()).hasSize(2).extracting(ParameterBinding::getRequiredPosition)
				.containsOnly(1, 2);
		assertThat(query.getParameterBindings()).extracting(ParameterBinding::getOrigin)
				.extracting(ParameterOrigin::isExpression) //
				.containsOnly(true, true);
	}

	@Test // GH-3758
	void createsDistinctBindingsForNamedSpel() {

		StringQuery query = new StringQuery("select u from User u where u.firstname = :#{foo} OR u.firstname = :#{foo}",
				false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getParameterBindings()).hasSize(2).extracting(ParameterBinding::getOrigin)
				.extracting(ParameterOrigin::isExpression) //
				.containsOnly(true, true);
	}

	@Test // DATAJPA-461
	void detectsNamedInParameterBindings() {

		String queryString = "select u from User u where u.id in :ids";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(queryString);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		assertNamedBinding(InParameterBinding.class, "ids", bindings.get(0));
	}

	@Test // DATAJPA-461
	void detectsMultipleNamedInParameterBindings() {

		String queryString = "select u from User u where u.id in :ids and u.name in :names and foo = :bar";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(queryString);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);

		assertNamedBinding(InParameterBinding.class, "ids", bindings.get(0));
		assertNamedBinding(InParameterBinding.class, "names", bindings.get(1));
		assertNamedBinding(ParameterBinding.class, "bar", bindings.get(2));
	}

	@Test // GH-3784
	void deriveCountQueryWithNamedInRetainsOrigin() {

		String queryString = "select u from User u where (:logins) IS NULL OR LOWER(u.login) IN (:logins)";
		DeclaredQuery query = new StringQuery(queryString, false).deriveCountQuery(null);

		assertThat(query.getQueryString())
				.isEqualTo("select count(u) from User u where (:logins) IS NULL OR LOWER(u.login) IN (:logins_1)");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		assertNamedBinding(ParameterBinding.class, "logins", bindings.get(0));
		assertThat((MethodInvocationArgument) bindings.get(0).getOrigin()).extracting(MethodInvocationArgument::identifier)
				.extracting(BindingIdentifier::getName).isEqualTo("logins");

		assertNamedBinding(InParameterBinding.class, "logins_1", bindings.get(1));
		assertThat((MethodInvocationArgument) bindings.get(1).getOrigin()).extracting(MethodInvocationArgument::identifier)
				.extracting(BindingIdentifier::getName).isEqualTo("logins");
	}

	@Test // GH-3784
	void deriveCountQueryWithPositionalInRetainsOrigin() {

		String queryString = "select u from User u where (?1) IS NULL OR LOWER(u.login) IN (?1)";
		DeclaredQuery query = new StringQuery(queryString, false).deriveCountQuery(null);

		assertThat(query.getQueryString())
				.isEqualTo("select count(u) from User u where (?1) IS NULL OR LOWER(u.login) IN (?2)");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		assertPositionalBinding(ParameterBinding.class, 1, bindings.get(0));
		assertThat((MethodInvocationArgument) bindings.get(0).getOrigin()).extracting(MethodInvocationArgument::identifier)
				.extracting(BindingIdentifier::getPosition).isEqualTo(1);

		assertPositionalBinding(InParameterBinding.class, 2, bindings.get(1));
		assertThat((MethodInvocationArgument) bindings.get(1).getOrigin()).extracting(MethodInvocationArgument::identifier)
				.extracting(BindingIdentifier::getPosition).isEqualTo(1);
	}

	@Test // DATAJPA-461
	void detectsPositionalInParameterBindings() {

		String queryString = "select u from User u where u.id in ?1";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(queryString);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		assertPositionalBinding(InParameterBinding.class, 1, bindings.get(0));
	}

	@Test // GH-3126
	void allowsReuseOfParameterWithInAndRegularBinding() {

		StringQuery query = new StringQuery(
				"select u from User u where COALESCE(?1) is null OR u.id in ?1 OR COALESCE(?1) is null OR u.id in ?1", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where COALESCE(?1) is null OR u.id in ?2 OR COALESCE(?1) is null OR u.id in ?2");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		assertPositionalBinding(ParameterBinding.class, 1, bindings.get(0));
		assertPositionalBinding(InParameterBinding.class, 2, bindings.get(1));

		query = new StringQuery(
				"select u from User u where COALESCE(:foo) is null OR u.id in :foo OR COALESCE(:foo) is null OR u.id in :foo",
				true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(
				"select u from User u where COALESCE(:foo) is null OR u.id in :foo_1 OR COALESCE(:foo) is null OR u.id in :foo_1");

		bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(2);

		assertNamedBinding(ParameterBinding.class, "foo", bindings.get(0));
		assertNamedBinding(InParameterBinding.class, "foo_1", bindings.get(1));
	}

	@Test // GH-3758
	void detectsPositionalInParameterBindingsAndExpressions() {

		String queryString = "select u from User u where foo = ?#{bar} and bar = ?3 and baz = ?#{baz}";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.getQueryString()).isEqualTo("select u from User u where foo = ?1 and bar = ?3 and baz = ?2");
	}

	@Test // GH-3758
	void detectsPositionalInParameterBindingsAndExpressionsWithReuse() {

		String queryString = "select u from User u where foo = ?#{bar} and bar = ?2 and baz = ?#{bar}";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.getQueryString()).isEqualTo("select u from User u where foo = ?1 and bar = ?2 and baz = ?3");
	}

	@Test // GH-3126
	void countQueryDerivationRetainsNamedExpressionParameters() {

		StringQuery query = new StringQuery(
				"select u from User u where foo = :#{bar} ORDER BY CASE WHEN (u.firstname >= :#{name}) THEN 0 ELSE 1 END",
				false);

		DeclaredQuery countQuery = query.deriveCountQuery(null);

		assertThat(countQuery.getParameterBindings()).hasSize(1);
		assertThat(countQuery.getParameterBindings()).extracting(ParameterBinding::getOrigin)
				.extracting(ParameterOrigin::isExpression).isEqualTo(List.of(true));

		query = new StringQuery(
				"select u from User u where foo = :#{bar} and bar = :bar ORDER BY CASE WHEN (u.firstname >= :bar) THEN 0 ELSE 1 END",
				false);

		countQuery = query.deriveCountQuery(null);

		assertThat(countQuery.getParameterBindings()).hasSize(2);
		assertThat(countQuery.getParameterBindings()) //
				.extracting(ParameterBinding::getOrigin) //
				.extracting(ParameterOrigin::isExpression).contains(true, false);
	}

	@Test // GH-3126
	void countQueryDerivationRetainsIndexedExpressionParameters() {

		StringQuery query = new StringQuery(
				"select u from User u where foo = ?#{bar} ORDER BY CASE WHEN (u.firstname >= ?#{name}) THEN 0 ELSE 1 END",
				false);

		DeclaredQuery countQuery = query.deriveCountQuery(null);

		assertThat(countQuery.getParameterBindings()).hasSize(1);
		assertThat(countQuery.getParameterBindings()).extracting(ParameterBinding::getOrigin)
				.extracting(ParameterOrigin::isExpression).isEqualTo(List.of(true));

		query = new StringQuery(
				"select u from User u where foo = ?#{bar} and bar = ?1 ORDER BY CASE WHEN (u.firstname >= ?1) THEN 0 ELSE 1 END",
				false);

		countQuery = query.deriveCountQuery(null);

		assertThat(countQuery.getParameterBindings()).hasSize(2);
		assertThat(countQuery.getParameterBindings()) //
				.extracting(ParameterBinding::getOrigin) //
				.extracting(ParameterOrigin::isExpression).contains(true, false);
	}

	@Test // DATAJPA-461
	void detectsMultiplePositionalInParameterBindings() {

		String queryString = "select u from User u where u.id in ?1 and u.names in ?2 and foo = ?3";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(queryString);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(3);

		assertPositionalBinding(InParameterBinding.class, 1, bindings.get(0));
		assertPositionalBinding(InParameterBinding.class, 2, bindings.get(1));
		assertPositionalBinding(ParameterBinding.class, 3, bindings.get(2));

	}

	@Test // DATAJPA-373
	void handlesMultipleNamedLikeBindingsCorrectly() {
		new StringQuery("select u from User u where u.firstname like %:firstname or foo like :bar", true);
	}

	@Test // DATAJPA-461
	void treatsGreaterThanBindingAsSimpleBinding() {

		StringQuery query = new StringQuery("select u from User u where u.createdDate > ?1", true);
		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertPositionalBinding(ParameterBinding.class, 1, bindings.get(0));

	}

	@Test // DATAJPA-473
	void removesLikeBindingsFromQueryIfQueryContainsSimpleBinding() {

		StringQuery query = new StringQuery("SELECT a FROM Article a WHERE a.overview LIKE %:escapedWord% ESCAPE '~'"
				+ " OR a.content LIKE %:escapedWord% ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(2);
		assertNamedBinding(LikeParameterBinding.class, "escapedWord", bindings.get(0));
		assertNamedBinding(ParameterBinding.class, "word", bindings.get(1));

		assertThat(query.getQueryString()).isEqualTo("SELECT a FROM Article a WHERE a.overview LIKE :escapedWord ESCAPE '~'"
				+ " OR a.content LIKE :escapedWord ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC");
	}

	@Test // DATAJPA-483
	void detectsInBindingWithParentheses() {

		StringQuery query = new StringQuery("select count(we) from MyEntity we where we.status in (:statuses)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "statuses", bindings.get(0));
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialFrenchCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where abonnés in (:abonnés)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "abonnés", bindings.get(0));
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where øre in (:øre)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "øre", bindings.get(0));
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialAsianCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where 생일 in (:생일)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "생일", bindings.get(0));
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialCharactersAndWordCharactersMixedInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where foo in (:ab1babc생일233)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "ab1babc생일233", bindings.get(0));
	}

	@Test // DATAJPA-712, GH-3619
	void shouldReplaceAllNamedExpressionParametersWithInClause() {

		StringQuery query = new StringQuery(
				"select a from A a where a.b in :#{#bs} and a.c in :#{#cs} and a.d in :${foo.bar}", true);
		String queryString = query.getQueryString();

		assertThat(queryString).isEqualTo(
				"select a from A a where a.b in :__$synthetic$__1 and a.c in :__$synthetic$__2 and a.d in :__$synthetic$__3");
	}

	@Test // DATAJPA-712
	void shouldReplaceExpressionWithLikeParameters() {

		StringQuery query = new StringQuery(
				"select a from A a where a.b LIKE :#{#filter.login}% and a.c LIKE %:#{#filter.login}", true);
		String queryString = query.getQueryString();

		assertThat(queryString)
				.isEqualTo("select a from A a where a.b LIKE :__$synthetic$__1 and a.c LIKE :__$synthetic$__2");
	}

	@Test // DATAJPA-712, GH-3619
	void shouldReplaceAllPositionExpressionParametersWithInClause() {

		StringQuery query = new StringQuery("select a from A a where a.b in ?#{#bs} and a.c in ?#{#cs} and a.d in ?${foo}",
				true);
		String queryString = query.getQueryString();

		assertThat(queryString).isEqualTo("select a from A a where a.b in ?1 and a.c in ?2 and a.d in ?3");

		assertThat(((Expression) query.getParameterBindings().get(0).getOrigin()).expression().getExpressionString())
				.isEqualTo("#bs");
		assertThat(((Expression) query.getParameterBindings().get(1).getOrigin()).expression().getExpressionString())
				.isEqualTo("#cs");
		assertThat(((Expression) query.getParameterBindings().get(2).getOrigin()).expression().getExpressionString())
				.isEqualTo("${foo}");
	}

	@Test // DATAJPA-864
	void detectsConstructorExpressions() {

		assertThat(
				new StringQuery("select  new  com.example.Dto(a.foo, a.bar)  from A a", false).hasConstructorExpression())
				.isTrue();
		assertThat(new StringQuery("select new com.example.Dto (a.foo, a.bar) from A a", false).hasConstructorExpression())
				.isTrue();
		assertThat(new StringQuery("select a from A a", true).hasConstructorExpression()).isFalse();
	}

	/**
	 * @see <a href="https://jakarta.ee/specifications/persistence/3.1/jakarta-persistence-spec-3.1#a5438">Jakarta
	 *      Persistence Specification: SELECT clause</a>
	 */
	@Test // DATAJPA-886
	void detectsConstructorExpressionForDefaultConstructor() {

		// Parentheses required
		assertThat(new StringQuery("select new com.example.Dto(a.name) from A a", false).hasConstructorExpression())
				.isTrue();
	}

	@Test // DATAJPA-1179
	void bindingsMatchQueryForIdenticalSpelExpressions() {

		StringQuery query = new StringQuery("select a from A a where a.first = :#{#exp} or a.second = :#{#exp}", true);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).isNotEmpty();

		for (ParameterBinding binding : bindings) {
			assertThat(binding.getName()).isNotNull();
			assertThat(query.getQueryString()).contains(binding.getName());
			assertThat(((Expression) binding.getOrigin()).expression().getExpressionString()).isEqualTo("#exp");
		}
	}

	@Test // DATAJPA-1235
	void getProjection() {

		checkProjection("SELECT something FROM Entity something", "something", "uppercase is supported", false);
		checkProjection("select something from Entity something", "something", "single expression", false);
		checkProjection("select x, y, z from Entity something", "x, y, z", "tuple", false);

		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> checkProjection("sect x, y, z from Entity something", "", "missing select", false));
		assertThatExceptionOfType(BadJpqlGrammarException.class)
				.isThrownBy(() -> checkProjection("select x, y, z fron Entity something", "", "missing from", false));
	}

	void checkProjection(String query, String expected, String description, boolean nativeQuery) {

		assertThat(new StringQuery(query, nativeQuery).getProjection()) //
				.as("%s (%s)", description, query) //
				.isEqualTo(expected);
	}

	@Test // DATAJPA-1235
	void getAlias() {

		checkAlias("from User u", "u", "simple query", false);
		checkAlias("select count(u) from User u", "u", "count query", true);
		checkAlias("select u from User as u where u.username = ?", "u", "with as", true);
		checkAlias("SELECT u FROM USER U", "U", "uppercase", false);
		checkAlias("select u from  User u", "u", "simple query", true);
		checkAlias("select u from  com.acme.User u", "u", "fully qualified package name", true);
		checkAlias("select u from T05User u", "u", "interesting entity name", true);
		checkAlias("from User ", null, "trailing space", false);
		checkAlias("from User", null, "no trailing space", false);
		checkAlias("from User as bs", "bs", "ignored as", false);
		checkAlias("from User as AS", "AS", "ignored as using the second", false);
		checkAlias("from User asas", "asas", "asas is weird but legal", false);
	}

	private void checkAlias(String query, String expected, String description, boolean nativeQuery) {

		assertThat(new StringQuery(query, nativeQuery).getAlias()) //
				.as("%s (%s)", description, query) //
				.isEqualTo(expected);
	}

	@Test // DATAJPA-1200
	void testHasNamedParameter() {

		checkHasNamedParameter("select something from x where id = :id", true, "named parameter", true);
		checkHasNamedParameter("in the :id middle", true, "middle", false);
		checkHasNamedParameter(":id start", true, "beginning", false);
		checkHasNamedParameter(":id", true, "alone", false);
		checkHasNamedParameter("select something from x where id = :id", true, "named parameter", true);
		checkHasNamedParameter(":UPPERCASE", true, "uppercase", false);
		checkHasNamedParameter(":lowercase", true, "lowercase", false);
		checkHasNamedParameter(":2something", true, "beginning digit", false);
		checkHasNamedParameter(":2", true, "only digit", false);
		checkHasNamedParameter(":.something", true, "dot", false);
		checkHasNamedParameter(":_something", true, "underscore", false);
		checkHasNamedParameter(":$something", true, "dollar", false);
		checkHasNamedParameter(":\uFE0F", true, "non basic latin emoji", false); //
		checkHasNamedParameter(":\u4E01", true, "chinese japanese korean", false);

		checkHasNamedParameter("no bind variable", false, "no bind variable", false);
		checkHasNamedParameter(":\u2004whitespace", false, "non basic latin whitespace", false);
		checkHasNamedParameter("select something from x where id = ?1", false, "indexed parameter", true);
		checkHasNamedParameter("::", false, "double colon", false);
		checkHasNamedParameter(":", false, "end of query", false);
		checkHasNamedParameter(":\u0003", false, "non-printable", false);
		checkHasNamedParameter(":*", false, "basic latin emoji", false);
		checkHasNamedParameter("\\:", false, "escaped colon", false);
		checkHasNamedParameter("::id", false, "double colon with identifier", false);
		checkHasNamedParameter("\\:id", false, "escaped colon with identifier", false);
		checkHasNamedParameter("select something from x where id = #something", false, "hash", true);
	}

	@Test // DATAJPA-1235
	void ignoresQuotedNamedParameterLookAlike() {

		checkNumberOfNamedParameters("select something from blah where x = '0:name'", 0, "single quoted", false);
		checkNumberOfNamedParameters("select something from blah where x = \"0:name\"", 0, "double quoted", false);
		// checkNumberOfNamedParameters("select something from blah where x = '\"0':name", 1, "double quote in single
		// quotes",
		// false);
		// checkNumberOfNamedParameters("select something from blah where x = \"'0\":name", 1, "single quote in double
		// quotes",
		// false);
	}

	@Test // DATAJPA-1307
	void detectsMultiplePositionalParameterBindingsWithoutIndex() {

		String queryString = "select u from User u where u.id in ? and u.names in ? and foo = ?";
		StringQuery query = new StringQuery(queryString, false);

		assertThat(query.getQueryString()).isEqualTo(queryString);
		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getParameterBindings()).hasSize(3);
	}

	@Test // DATAJPA-1307
	void failOnMixedBindingsWithoutIndex() {

		List<String> testQueries = Arrays.asList( //
				"something = ? and something = ?1", //
				"something = ?1 and something = ?", //
				"something = :name and something = ?", //
				"something = ?#{xx} and something = ?" //
		);

		for (String testQuery : testQueries) {

			Assertions.assertThatExceptionOfType(IllegalArgumentException.class) //
					.describedAs(testQuery).isThrownBy(() -> new StringQuery(testQuery, false));
		}
	}

	@Test // DATAJPA-1307
	void makesUsageOfJdbcStyleParameterAvailable() {

		assertThat(new StringQuery("from Something something where something = ?", false).usesJdbcStyleParameters())
				.isTrue();
		assertThat(new StringQuery("from Something something where something =?", false).usesJdbcStyleParameters())
				.isTrue();

		List<String> testQueries = Arrays.asList( //
				"from Something something where something = ?1", //
				"from Something something where something = :name", //
				"from Something something where something = ?#{xx}" //
		);

		for (String testQuery : testQueries) {

			assertThat(new StringQuery(testQuery, false) //
					.usesJdbcStyleParameters()) //
					.describedAs(testQuery) //
					.describedAs(testQuery) //
					.isFalse();
		}
	}

	@Test // DATAJPA-1307
	void questionMarkInStringLiteral() {

		String queryString = "select '? ' from dual";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.getQueryString()).isEqualTo(queryString);
		assertThat(query.hasParameterBindings()).isFalse();
		assertThat(query.getParameterBindings()).isEmpty();
		assertThat(query.usesJdbcStyleParameters()).isFalse();
	}

	@Test // DATAJPA-1318
	void isNotDefaultProjection() {

		List<String> queriesWithoutDefaultProjection = Arrays.asList( //
				"select a, b from C as c", //
				"SELECT a, b FROM C as c", //
				"SELECT a, b FROM C", //
				"SELECT a, b FROM C ", //
				"select a, b from C ", //
				"select a, b from C");

		for (String queryString : queriesWithoutDefaultProjection) {
			assertThat(new StringQuery(queryString, true).isDefaultProjection()) //
					.describedAs(queryString) //
					.isFalse();
		}

		List<String> queriesWithDefaultProjection = Arrays.asList( //
				"select c from C as c", //
				"SELECT c FROM C as c", //
				"SELECT c FROM C as c ", //
				"SELECT c  FROM C as c", //
				"SELECT  c FROM C as c", //
				"SELECT  c FROM C as C", //
				"SELECT  C FROM C as c", //
				"SELECT  C FROM C as C" //
		);

		for (String queryString : queriesWithDefaultProjection) {
			assertThat(new StringQuery(queryString, true).isDefaultProjection()) //
					.describedAs(queryString) //
					.isTrue();
		}
	}

	@Test // GH-3125
	void questionMarkInStringLiteralWithParameters() {

		String queryString = "SELECT CAST(REGEXP_SUBSTR(itp.template_as_txt, '(?<=templateId\\\\\\\\=)(\\\\\\\\d+)(?:\\\\\\\\R)') AS INT) AS templateId FROM foo itp WHERE bar = ?1 AND baz = 1";
		StringQuery query = new StringQuery(queryString, false);

		assertThat(query.getQueryString()).isEqualTo(queryString);
		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getParameterBindings()).hasSize(1);
		assertThat(query.usesJdbcStyleParameters()).isFalse();
	}

	@Test // DATAJPA-1652
	void usingPipesWithNamedParameter() {

		String queryString = "SELECT u FROM User u WHERE u.lastname LIKE '%'||:name||'%'";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.getParameterBindings()) //
				.extracting(ParameterBinding::getName) //
				.containsExactly("name");
	}

	@Test // DATAJPA-1652
	void usingGreaterThanWithNamedParameter() {

		String queryString = "SELECT u FROM User u WHERE :age>u.age";
		StringQuery query = new StringQuery(queryString, true);

		assertThat(query.getParameterBindings()) //
				.extracting(ParameterBinding::getName) //
				.containsExactly("age");
	}

	void checkNumberOfNamedParameters(String query, int expectedSize, String label, boolean nativeQuery) {

		EntityQuery introspectedQuery = nativeQuery
				? EntityQuery.introspectNativeQuery(query, QueryEnhancerSelector.DEFAULT_SELECTOR)
				: EntityQuery.introspectJpql(query, QueryEnhancerSelector.DEFAULT_SELECTOR);

		assertThat(introspectedQuery.hasNamedParameter()) //
				.describedAs("hasNamed Parameter " + label) //
				.isEqualTo(expectedSize > 0);
		assertThat(introspectedQuery.getParameterBindings()) //
				.describedAs("parameterBindings " + label) //
				.hasSize(expectedSize);
	}

	private void checkHasNamedParameter(String query, boolean expected, String label, boolean nativeQuery) {

		DeclaredQuery source = nativeQuery ? DeclaredQuery.nativeQuery(query) : DeclaredQuery.jpqlQuery(query);
		BindableQuery bindableQuery = ParameterBindingParser.INSTANCE.parseParameterBindingsOfQueryIntoBindingsAndReturnCleanedQuery(source);

		assertThat(bindableQuery.getBindings().stream().anyMatch(it -> it.getIdentifier().hasName())) //
				.describedAs(String.format("<%s> (%s)", query, label)) //
				.isEqualTo(expected);
	}

	private void assertPositionalBinding(Class<? extends ParameterBinding> bindingType, Integer position,
			ParameterBinding expectedBinding) {

		assertThat(bindingType.isInstance(expectedBinding)).isTrue();
		assertThat(expectedBinding).isNotNull();
		assertThat(expectedBinding.getPosition()).isEqualTo(position);
	}

	private void assertNamedBinding(Class<? extends ParameterBinding> bindingType, String parameterName,
			ParameterBinding expectedBinding) {

		assertThat(bindingType.isInstance(expectedBinding)).isTrue();
		assertThat(expectedBinding).isNotNull();
		assertThat(expectedBinding.getName()).isEqualTo(parameterName);
	}
}
