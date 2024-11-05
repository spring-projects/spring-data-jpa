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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.AbstractJpqlQuery;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Entity;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Expression;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Join;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.OrderExpression;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Origin;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.ParameterPlaceholder;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.PathAndOrigin;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.Predicate;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.RenderContext;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.SelectStep;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.WhereStep;

/**
 * @author Christoph Strobl
 */
class JpqlQueryBuilderUnitTests {

	@Test
	void placeholdersRenderCorrectly() {

		assertThat(JpqlQueryBuilder.parameter(ParameterPlaceholder.indexed(1)).render(RenderContext.EMPTY)).isEqualTo("?1");
		assertThat(JpqlQueryBuilder.parameter(ParameterPlaceholder.named("arg1")).render(RenderContext.EMPTY))
				.isEqualTo(":arg1");
		assertThat(JpqlQueryBuilder.parameter("?1").render(RenderContext.EMPTY)).isEqualTo("?1");
	}

	@Test
	void placeholdersErrorOnInvaludInput() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> JpqlQueryBuilder.parameter((String) null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> JpqlQueryBuilder.parameter(""));
	}

	@Test
	void stringLiteralRendersAsQuotedString() {

		assertThat(JpqlQueryBuilder.stringLiteral("literal").render(RenderContext.EMPTY)).isEqualTo("'literal'");

		/* JPA Spec - 4.6.1 Literals:
		   > A string literal that includes a single quote is represented by two single quotes--for example: 'literal''s'. */
		assertThat(JpqlQueryBuilder.stringLiteral("literal's").render(RenderContext.EMPTY)).isEqualTo("'literal''s'");
	}

	@Test
	void entity() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		assertThat(entity.alias()).isEqualTo("o");
		assertThat(entity.entity()).isEqualTo(Order.class.getName());
		assertThat(entity.getName()).isEqualTo(Order.class.getSimpleName()); // TODO: this really confusing
		assertThat(entity.simpleName()).isEqualTo(Order.class.getSimpleName());
	}

	@Test
	void literalExpressionRendersAsIs() {
		Expression expression = JpqlQueryBuilder.expression("CONCAT(person.lastName, ‘, ’, person.firstName))");
		assertThat(expression.render(RenderContext.EMPTY)).isEqualTo("CONCAT(person.lastName, ‘, ’, person.firstName))");
	}

	@Test
	void xxx() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		PathAndOrigin orderDate = JpqlQueryBuilder.path(entity, "date");

		String fragment = JpqlQueryBuilder.where(orderDate).eq("{d '2024-11-05'}").render(ctx(entity));

		assertThat(fragment).isEqualTo("o.date = {d '2024-11-05'}");

		// JpqlQueryBuilder.where(PathAndOrigin)
	}

	@Test
	void predicateRendering() {


		Entity entity = JpqlQueryBuilder.entity(Order.class);
		WhereStep where = JpqlQueryBuilder.where(JpqlQueryBuilder.path(entity, "country"));

		assertThat(where.between("'AT'", "'DE'").render(ctx(entity))).isEqualTo("o.country BETWEEN 'AT' AND 'DE'");
		assertThat(where.eq("'AT'").render(ctx(entity))).isEqualTo("o.country = 'AT'");
		assertThat(where.eq(JpqlQueryBuilder.stringLiteral("AT")).render(ctx(entity))).isEqualTo("o.country = 'AT'");
		assertThat(where.gt("'AT'").render(ctx(entity))).isEqualTo("o.country > 'AT'");
		assertThat(where.gte("'AT'").render(ctx(entity))).isEqualTo("o.country >= 'AT'");
		// TODO: that is really really bad
		// lange namen
		assertThat(where.in("'AT', 'DE'").render(ctx(entity))).isEqualTo("o.country IN ('AT', 'DE')");

		// 1 in age - cleanup what is not used - remove everything eles
		// assertThat(where.inMultivalued("'AT', 'DE'").render(ctx(entity))).isEqualTo("o.country IN ('AT', 'DE')"); //
		assertThat(where.isEmpty().render(ctx(entity))).isEqualTo("o.country IS EMPTY");
		assertThat(where.isNotEmpty().render(ctx(entity))).isEqualTo("o.country IS NOT EMPTY");
		assertThat(where.isTrue().render(ctx(entity))).isEqualTo("o.country = TRUE");
		assertThat(where.isFalse().render(ctx(entity))).isEqualTo("o.country = FALSE");
		assertThat(where.isNull().render(ctx(entity))).isEqualTo("o.country IS NULL");
		assertThat(where.isNotNull().render(ctx(entity))).isEqualTo("o.country IS NOT NULL");
		assertThat(where.like("'\\_%'", "" + EscapeCharacter.DEFAULT.getEscapeCharacter()).render(ctx(entity)))
				.isEqualTo("o.country LIKE '\\_%' ESCAPE '\\'");
		assertThat(where.notLike("'\\_%'", "" + EscapeCharacter.DEFAULT.getEscapeCharacter()).render(ctx(entity)))
				.isEqualTo("o.country NOT LIKE '\\_%' ESCAPE '\\'");
		assertThat(where.lt("'AT'").render(ctx(entity))).isEqualTo("o.country < 'AT'");
		assertThat(where.lte("'AT'").render(ctx(entity))).isEqualTo("o.country <= 'AT'");
		assertThat(where.memberOf("'AT'").render(ctx(entity))).isEqualTo("'AT' MEMBER OF o.country");
		// TODO: can we have this where.value(foo).memberOf(pathAndOrigin);
		assertThat(where.notMemberOf("'AT'").render(ctx(entity))).isEqualTo("'AT' NOT MEMBER OF o.country");
		assertThat(where.neq("'AT'").render(ctx(entity))).isEqualTo("o.country != 'AT'");
	}

	@Test
	void selectRendering() {

		// make sure things are immutable
		SelectStep select = JpqlQueryBuilder.selectFrom(Order.class); // the select step is mutable - not sure i like it
		// hm, I somehow exepect this to render only the selection part
		assertThat(select.count().render()).startsWith("SELECT COUNT(o)");
		assertThat(select.distinct().entity().render()).startsWith("SELECT DISTINCT o ");
		assertThat(select.distinct().count().render()).startsWith("SELECT COUNT(DISTINCT o) ");
		assertThat(JpqlQueryBuilder.selectFrom(Order.class).select(JpqlQueryBuilder.path(JpqlQueryBuilder.entity(Order.class), "country")).render())
			.startsWith("SELECT o.country ");
	}

//	@Test
//	void sorting() {
//
//		JpqlQueryBuilder.orderBy(new OrderExpression() , Sort.Order.asc("country"));
//
//		Entity entity = JpqlQueryBuilder.entity(Order.class);
//
//		AbstractJpqlQuery query = JpqlQueryBuilder.selectFrom(Order.class)
//			.entity()
//			.orderBy()
//			.where(context -> "1 = 1");
//
//	}

	@Test
	void joins() {

		Entity entity = JpqlQueryBuilder.entity(LineItem.class);
		Join li_pr = JpqlQueryBuilder.innerJoin(entity, "product");
		Join li_pr2 = JpqlQueryBuilder.innerJoin(entity, "product2");

		PathAndOrigin productName = JpqlQueryBuilder.path(li_pr, "name");
		PathAndOrigin personName = JpqlQueryBuilder.path(li_pr2, "name");

		String fragment = JpqlQueryBuilder.where(productName).eq(JpqlQueryBuilder.stringLiteral("ex30"))
				.and(JpqlQueryBuilder.where(personName).eq(JpqlQueryBuilder.stringLiteral("ex40"))).render(ctx(entity));

		assertThat(fragment).isEqualTo("p.name = 'ex30' AND join_0.name = 'ex40'");
	}

	@Test
	void x2() {

		Entity entity = JpqlQueryBuilder.entity(LineItem.class);
		Join li_pr = JpqlQueryBuilder.innerJoin(entity, "product");
		Join li_pe = JpqlQueryBuilder.innerJoin(entity, "person");

		PathAndOrigin productName = JpqlQueryBuilder.path(li_pr, "name");
		PathAndOrigin personName = JpqlQueryBuilder.path(li_pe, "name");

		String fragment = JpqlQueryBuilder.where(productName).eq(JpqlQueryBuilder.stringLiteral("ex30"))
				.and(JpqlQueryBuilder.where(personName).eq(JpqlQueryBuilder.stringLiteral("cstrobl"))).render(ctx(entity));

		assertThat(fragment).isEqualTo("p.name = 'ex30' AND join_0.name = 'cstrobl'");
	}

	@Test
	void x3() {

		Entity entity = JpqlQueryBuilder.entity(LineItem.class);
		Join li_pr = JpqlQueryBuilder.innerJoin(entity, "product");
		Join li_pe = JpqlQueryBuilder.innerJoin(entity, "person");

		PathAndOrigin productName = JpqlQueryBuilder.path(li_pr, "name");
		PathAndOrigin personName = JpqlQueryBuilder.path(li_pe, "name");

		// JpqlQueryBuilder.and("x = y", "a = b"); -> x = y AND a = b

		// JpqlQueryBuilder.nested(JpqlQueryBuilder.and("x = y", "a = b")) (x = y AND a = b)

		String fragment = JpqlQueryBuilder.where(productName).eq(JpqlQueryBuilder.stringLiteral("ex30"))
				.and(JpqlQueryBuilder.where(personName).eq(JpqlQueryBuilder.stringLiteral("cstrobl"))).render(ctx(entity));

		assertThat(fragment).isEqualTo("p.name = 'ex30' AND join_0.name = 'cstrobl'");
	}

	static RenderContext ctx(Entity... entities) {
		Map<Origin, String> aliases = new LinkedHashMap<>(entities.length);
		for (Entity entity : entities) {
			aliases.put(entity, entity.alias());
		}

		return new RenderContext(aliases);
	}

	@jakarta.persistence.Entity
	static class Order {

		@Id Long id;
		Date date;
		String country;

		@OneToMany List<LineItem> lineItems;
	}

	@jakarta.persistence.Entity
	static class LineItem {

		@Id Long id;

		@ManyToOne Product product;
		@ManyToOne Product product2;
		@ManyToOne Product person;

	}

	@jakarta.persistence.Entity
	static class Person {
		@Id Long id;
		String name;
	}

	@jakarta.persistence.Entity
	static class Product {

		@Id Long id;

		String name;
		String productType;

	}
}
