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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * Unit tests for {@link DtoProjectionTransformerDelegate}.
 *
 * @author Mark Paluch
 */
class JpqlDtoQueryTransformerUnitTests {

	JpaQueryMethod method = getMethod("dtoProjection");
	JpqlSortedQueryTransformer transformer = new JpqlSortedQueryTransformer(Sort.unsorted(), null,
			method.getResultProcessor().getReturnedType());

	@Test // GH-3076
	void shouldTranslateSingleProjectionToDto() {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery("SELECT p from Person p");

		QueryTokenStream visit = transformer.visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT new org.springframework.data.jpa.repository.query.JpqlDtoQueryTransformerUnitTests$MyRecord(p.foo, p.bar) from Person p");
	}

//	@Test // GH-3076
//	void xxx() {
//
//		JpaQueryMethod method = getMethod("dtoProjection2");
//		JpqlSortedQueryTransformer transformer = new JpqlSortedQueryTransformer(Sort.unsorted(), null,
//			method.getResultProcessor().getReturnedType());
//
//		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser.parseQuery("select u.foo, u.bar, count(r) from User u left outer join u.role r group by u");
//
//		QueryTokenStream visit = transformer.visit(parser.getContext());
//
//		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
//			"select new org.springframework.data.jpa.repository.query.JpqlDtoQueryTransformerUnitTests$MyRecord2(u.foo, u.bar, count(r)) from User u left outer join u.role r group by u");
//	}

	@Test // GH-3076
	void shouldRewriteQueriesWithSubselect() {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser
				.parseQuery("select u from User u left outer join u.roles r where r in (select r from Role r)");

		QueryTokenStream visit = transformer.visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"select new org.springframework.data.jpa.repository.query.JpqlDtoQueryTransformerUnitTests$MyRecord(u.foo, u.bar) from User u left outer join u.roles r where r in (select r from Role r)");
	}

	@Test // GH-3076
	void shouldNotTranslateConstructorExpressionQuery() {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser
				.parseQuery("SELECT NEW String(p) from Person p");

		QueryTokenStream visit = transformer.visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo("SELECT NEW String(p) from Person p");
	}

	@Test
	void shouldTranslatePropertySelectionToDto() {

		JpaQueryEnhancer.JpqlQueryParser parser = JpaQueryEnhancer.JpqlQueryParser
				.parseQuery("SELECT p.foo, p.bar, sum(p.age) from Person p");

		QueryTokenStream visit = transformer.visit(parser.getContext());

		assertThat(QueryRenderer.TokenRenderer.render(visit)).isEqualTo(
				"SELECT new org.springframework.data.jpa.repository.query.JpqlDtoQueryTransformerUnitTests$MyRecord(p.foo, p.bar, sum(p.age)) from Person p");
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

	interface MyRepo extends Repository<Person, String> {

		MyRecord dtoProjection();
		MyRecord2 dtoProjection2();
	}

	record Person(String id) {

	}

	record MyRecord(String foo, String bar) {

	}

	record MyRecord2(String foo, String bar, Integer count) {

	}
}
