/*
 * Copyright 2024-present the original author or authors.
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

import java.lang.reflect.Method;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Support class for unit tests for {@link DtoProjectionTransformerDelegate}.
 *
 * @author Mark Paluch
 */
abstract class AbstractDtoQueryTransformerUnitTests<P extends JpaQueryEnhancer<? extends QueryInformation>> {

	JpaQueryMethod method = getMethod("dtoProjection");

	@Test // GH-3076
	void shouldRewritePrimarySelectionToConstructorExpressionWithProperties() {

		P parser = parse("SELECT p from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT new org.springframework.data.jpa.repository.query.AbstractDtoQueryTransformerUnitTests$MyRecord(p.foo, p.bar) from Person p");
	}

	@Test // GH-3076, GH-3895
	void shouldRewriteSelectionToConstructorExpression() {

		P parser = parse("SELECT p.name from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT p.name from Person p");
	}

	@Test // GH-3076
	void shouldRewriteQueriesWithSubselect() {

		P parser = parse("select u from User u left outer join u.roles r where r in (select r from Role r)");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"select new org.springframework.data.jpa.repository.query.AbstractDtoQueryTransformerUnitTests$MyRecord(u.foo, u.bar) from User u left outer join u.roles r where r in (select r from Role r)");
	}

	@Test // GH-3076
	void shouldNotRewriteQueriesWithoutProperties() {

		JpaQueryMethod method = getMethod("noProjection");
		P parser = parse("select u from User u");

		QueryTokenStream visit = getTransformer(parser, method).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo("select u from User u");
	}

	@Test // GH-3076
	void shouldNotTranslateConstructorExpressionQuery() {

		P parser = parse("SELECT NEW com.foo(p) from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo("SELECT NEW com.foo(p) from Person p");
	}

	@Test // GH-3076
	void shouldTranslatePropertySelectionToDto() {

		P parser = parse("SELECT p.foo, p.bar, sum(p.age) from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT p.foo, p.bar, sum(p.age) from Person p");
	}

	@Test // GH-3895
	void shouldStripAliasesFromDtoProjection() {

		P parser = parse("SELECT sum(p.age) As age, p.foo as foo, p.bar AS bar from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT sum(p.age) As age, p.foo as foo, p.bar AS bar from Person p");
	}

	@Test // GH-3895
	void shouldStripAliasesFromDtoProjectionWithSubquery() {

		P parser = parse(
				"SELECT p.foo as foo, p.bar AS bar, cast(p.age as INTEGER) As age, (SELECT b.foo FROM Bar AS b) from Person p");

		QueryTokenStream visit = getTransformer(parser).visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT p.foo as foo, p.bar AS bar, cast(p.age as INTEGER) As age, (SELECT b.foo FROM Bar AS b) from Person p");
	}

	private JpaQueryMethod getMethod(String name, Class<?>... parameterTypes) {

		try {
			Method method = MyRepo.class.getMethod(name, parameterTypes);
			PersistenceProvider persistenceProvider = PersistenceProvider.HIBERNATE;

			return new JpaQueryMethod(method, new DefaultRepositoryMetadata(MyRepo.class),
					new SpelAwareProxyProjectionFactory(), persistenceProvider);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	abstract P parse(String query);

	private ParseTreeVisitor<QueryTokenStream> getTransformer(P parser) {
		return getTransformer(parser, method);
	}

	abstract ParseTreeVisitor<QueryTokenStream> getTransformer(P parser, QueryMethod method);

	interface MyRepo extends Repository<Person, String> {

		MyRecord dtoProjection();

		EmptyClass noProjection();
	}

	record Person(String id) {

	}

	record MyRecord(String foo, String bar) {

	}

	static class EmptyClass {

	}
}
