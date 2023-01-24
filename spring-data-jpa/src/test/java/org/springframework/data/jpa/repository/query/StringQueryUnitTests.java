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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.query.StringQuery.InParameterBinding;
import org.springframework.data.jpa.repository.query.StringQuery.LikeParameterBinding;
import org.springframework.data.jpa.repository.query.StringQuery.ParameterBinding;
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
 */
class StringQueryUnitTests {

	private SoftAssertions softly = new SoftAssertions();

	@Test // DATAJPA-341
	void doesNotConsiderPlainLikeABinding() {

		String source = "select from User u where u.firstname like :firstname";
		StringQuery query = new StringQuery(source, false);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo(source);

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding.getType()).isEqualTo(Type.LIKE);

		assertThat(binding.hasName("firstname")).isTrue();
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
		assertThat(binding.hasPosition(1)).isTrue();
		assertThat(binding.getType()).isEqualTo(Type.CONTAINING);

		binding = (LikeParameterBinding) bindings.get(1);
		assertThat(binding).isNotNull();
		assertThat(binding.hasPosition(2)).isTrue();
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);
	}

	@Test // DATAJPA-292
	void detectsNamedLikeBindings() {

		StringQuery query = new StringQuery("select u from User u where u.firstname like %:firstname", true);

		assertThat(query.hasParameterBindings()).isTrue();
		assertThat(query.getQueryString()).isEqualTo("select u from User u where u.firstname like :firstname");

		List<ParameterBinding> bindings = query.getParameterBindings();
		assertThat(bindings).hasSize(1);

		LikeParameterBinding binding = (LikeParameterBinding) bindings.get(0);
		assertThat(binding).isNotNull();
		assertThat(binding.hasName("firstname")).isTrue();
		assertThat(binding.getType()).isEqualTo(Type.ENDING_WITH);
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

		softly.assertAll();
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

		softly.assertAll();
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

		softly.assertAll();
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

		softly.assertAll();
	}

	@Test // DATAJPA-373
	void handlesMultipleNamedLikeBindingsCorrectly() {
		new StringQuery("select u from User u where u.firstname like %:firstname or foo like :bar", true);
	}

	@Test // DATAJPA-292, DATAJPA-362
	void rejectsDifferentBindingsForRepeatedParameter() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new StringQuery("select u from User u where u.firstname like %?1 and u.lastname like ?1%", true));
	}

	@Test // DATAJPA-461
	void treatsGreaterThanBindingAsSimpleBinding() {

		StringQuery query = new StringQuery("select u from User u where u.createdDate > ?1", true);
		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertPositionalBinding(ParameterBinding.class, 1, bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-473
	void removesLikeBindingsFromQueryIfQueryContainsSimpleBinding() {

		StringQuery query = new StringQuery("SELECT a FROM Article a WHERE a.overview LIKE %:escapedWord% ESCAPE '~'"
				+ " OR a.content LIKE %:escapedWord% ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(2);
		assertNamedBinding(LikeParameterBinding.class, "escapedWord", bindings.get(0));
		assertNamedBinding(ParameterBinding.class, "word", bindings.get(1));

		softly.assertThat(query.getQueryString())
				.isEqualTo("SELECT a FROM Article a WHERE a.overview LIKE :escapedWord ESCAPE '~'"
						+ " OR a.content LIKE :escapedWord ESCAPE '~' OR a.title = :word ORDER BY a.articleId DESC");

		softly.assertAll();
	}

	@Test // DATAJPA-483
	void detectsInBindingWithParentheses() {

		StringQuery query = new StringQuery("select count(we) from MyEntity we where we.status in (:statuses)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "statuses", bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialFrenchCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where abonnés in (:abonnés)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "abonnés", bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where øre in (:øre)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "øre", bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialAsianCharactersInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where 생일 in (:생일)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "생일", bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-545
	void detectsInBindingWithSpecialCharactersAndWordCharactersMixedInParentheses() {

		StringQuery query = new StringQuery("select * from MyEntity where foo in (:ab1babc생일233)", true);

		List<ParameterBinding> bindings = query.getParameterBindings();

		assertThat(bindings).hasSize(1);
		assertNamedBinding(InParameterBinding.class, "ab1babc생일233", bindings.get(0));

		softly.assertAll();
	}

	@Test // DATAJPA-362
	void rejectsDifferentBindingsForRepeatedParameter2() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new StringQuery("select u from User u where u.firstname like ?1 and u.lastname like %?1", true));
	}

	@Test // DATAJPA-712
	void shouldReplaceAllNamedExpressionParametersWithInClause() {

		StringQuery query = new StringQuery("select a from A a where a.b in :#{#bs} and a.c in :#{#cs}", true);
		String queryString = query.getQueryString();

		assertThat(queryString).isEqualTo("select a from A a where a.b in :__$synthetic$__1 and a.c in :__$synthetic$__2");
	}

	@Test // DATAJPA-712
	void shouldReplaceAllPositionExpressionParametersWithInClause() {

		StringQuery query = new StringQuery("select a from A a where a.b in ?#{#bs} and a.c in ?#{#cs}", true);
		String queryString = query.getQueryString();

		softly.assertThat(queryString).isEqualTo("select a from A a where a.b in ?1 and a.c in ?2");
		softly.assertThat(query.getParameterBindings().get(0).getExpression()).isEqualTo("#bs");
		softly.assertThat(query.getParameterBindings().get(1).getExpression()).isEqualTo("#cs");

		softly.assertAll();
	}

	@Test // DATAJPA-864
	void detectsConstructorExpressions() {

		softly.assertThat(new StringQuery("select  new  Dto(a.foo, a.bar)  from A a", false).hasConstructorExpression())
				.isTrue();
		softly.assertThat(new StringQuery("select new Dto (a.foo, a.bar) from A a", false).hasConstructorExpression())
				.isTrue();
		softly.assertThat(new StringQuery("select a from A a", true).hasConstructorExpression()).isFalse();

		softly.assertAll();
	}

	/**
	 * @see <a href="download.oracle.com/otn-pub/jcp/persistence-2_1-fr-eval-spec/JavaPersistence.pdf">JPA 2.1
	 *      specification, section 4.8</a>
	 */
	@Test // DATAJPA-886
	void detectsConstructorExpressionForDefaultConstructor() {

		// Parentheses required
		softly.assertThat(new StringQuery("select new Dto() from A a", false).hasConstructorExpression()).isTrue();
		softly.assertThat(new StringQuery("select new Dto from A a", false).hasConstructorExpression()).isFalse();

		softly.assertAll();
	}

	@Test // DATAJPA-1179
	void bindingsMatchQueryForIdenticalSpelExpressions() {

		StringQuery query = new StringQuery("select a from A a where a.first = :#{#exp} or a.second = :#{#exp}", true);

		List<ParameterBinding> bindings = query.getParameterBindings();
		softly.assertThat(bindings).isNotEmpty();

		for (ParameterBinding binding : bindings) {
			softly.assertThat(binding.getName()).isNotNull();
			softly.assertThat(query.getQueryString()).contains(binding.getName());
			softly.assertThat(binding.getExpression()).isEqualTo("#exp");
		}

		softly.assertAll();
	}

	@Test // DATAJPA-1235
	void getProjection() {

		checkProjection("SELECT something FROM", "something", "uppercase is supported", false);
		checkProjection("select something from", "something", "single expression", false);
		checkProjection("select x, y, z from", "x, y, z", "tuple", false);
		checkProjection("sect x, y, z from", "", "missing select", false);
		checkProjection("select x, y, z fron", "", "missing from", false);

		softly.assertAll();
	}

	void checkProjection(String query, String expected, String description, boolean nativeQuery) {

		softly.assertThat(new StringQuery(query, nativeQuery).getProjection()) //
				.as("%s (%s)", description, query) //
				.isEqualTo(expected);
	}

	@Test // DATAJPA-1235
	void getAlias() {

		checkAlias("from User u", "u", "simple query", false);
		checkAlias("select count(u) from User u", "u", "count query", true);
		checkAlias("select u from User as u where u.username = ?", "u", "with as", true);
		checkAlias("SELECT FROM USER U", "U", "uppercase", false);
		checkAlias("select u from  User u", "u", "simple query", true);
		checkAlias("select u from  com.acme.User u", "u", "fully qualified package name", true);
		checkAlias("select u from T05User u", "u", "interesting entity name", true);
		checkAlias("from User ", null, "trailing space", false);
		checkAlias("from User", null, "no trailing space", false);
		checkAlias("from User as bs", "bs", "ignored as", false);
		checkAlias("from User as AS", "AS", "ignored as using the second", false);
		checkAlias("from User asas", "asas", "asas is weird but legal", false);

		softly.assertAll();
	}

	private void checkAlias(String query, String expected, String description, boolean nativeQuery) {

		softly.assertThat(new StringQuery(query, nativeQuery).getAlias()) //
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

		softly.assertAll();
	}

	@Test // DATAJPA-1235
	void ignoresQuotedNamedParameterLookAlike() {

		checkNumberOfNamedParameters("select something from blah where x = '0:name'", 0, "single quoted", false);
		checkNumberOfNamedParameters("select something from blah where x = \"0:name\"", 0, "double quoted", false);
		checkNumberOfNamedParameters("select something from blah where x = '\"0':name", 1, "double quote in single quotes",
				false);
		checkNumberOfNamedParameters("select something from blah where x = \"'0\":name", 1, "single quote in double quotes",
				false);

		softly.assertAll();
	}

	@Test // DATAJPA-1307
	void detectsMultiplePositionalParameterBindingsWithoutIndex() {

		String queryString = "select u from User u where u.id in ? and u.names in ? and foo = ?";
		StringQuery query = new StringQuery(queryString, false);

		softly.assertThat(query.getQueryString()).isEqualTo(queryString);
		softly.assertThat(query.hasParameterBindings()).isTrue();
		softly.assertThat(query.getParameterBindings()).hasSize(3);

		softly.assertAll();
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

		softly.assertThat(new StringQuery("something = ?", false).usesJdbcStyleParameters()).isTrue();

		List<String> testQueries = Arrays.asList( //
				"something = ?1", //
				"something = :name", //
				"something = ?#{xx}" //
		);

		for (String testQuery : testQueries) {

			softly.assertThat(new StringQuery(testQuery, false) //
					.usesJdbcStyleParameters()) //
					.describedAs(testQuery) //
					.isFalse();
		}

		softly.assertAll();
	}

	@Test // DATAJPA-1307
	void questionMarkInStringLiteral() {

		String queryString = "select '? ' from dual";
		StringQuery query = new StringQuery(queryString, false);

		softly.assertThat(query.getQueryString()).isEqualTo(queryString);
		softly.assertThat(query.hasParameterBindings()).isFalse();
		softly.assertThat(query.getParameterBindings()).isEmpty();

		softly.assertAll();
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
			softly.assertThat(new StringQuery(queryString, true).isDefaultProjection()) //
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
			softly.assertThat(new StringQuery(queryString, true).isDefaultProjection()) //
					.describedAs(queryString) //
					.isTrue();
		}

		softly.assertAll();
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

		DeclaredQuery declaredQuery = DeclaredQuery.of(query, nativeQuery);

		softly.assertThat(declaredQuery.hasNamedParameter()) //
				.describedAs("hasNamed Parameter " + label) //
				.isEqualTo(expectedSize > 0);
		softly.assertThat(declaredQuery.getParameterBindings()) //
				.describedAs("parameterBindings " + label) //
				.hasSize(expectedSize);
	}

	private void checkHasNamedParameter(String query, boolean expected, String label, boolean nativeQuery) {

		softly.assertThat(new StringQuery(query, nativeQuery).hasNamedParameter()) //
				.describedAs(String.format("<%s> (%s)", query, label)) //
				.isEqualTo(expected);
	}

	private void assertPositionalBinding(Class<? extends ParameterBinding> bindingType, Integer position,
			ParameterBinding expectedBinding) {

		softly.assertThat(bindingType.isInstance(expectedBinding)).isTrue();
		softly.assertThat(expectedBinding).isNotNull();
		softly.assertThat(expectedBinding.hasPosition(position)).isTrue();
	}

	private void assertNamedBinding(Class<? extends ParameterBinding> bindingType, String parameterName,
			ParameterBinding expectedBinding) {

		softly.assertThat(bindingType.isInstance(expectedBinding)).isTrue();
		softly.assertThat(expectedBinding).isNotNull();
		softly.assertThat(expectedBinding.hasName(parameterName)).isTrue();
	}
}
