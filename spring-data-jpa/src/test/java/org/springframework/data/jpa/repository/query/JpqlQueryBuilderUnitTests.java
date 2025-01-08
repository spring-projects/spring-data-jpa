/*
 * Copyright 2024-2025 the original author or authors.
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
import static org.springframework.data.jpa.repository.query.JpqlQueryBuilder.*;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpqlQueryBuilder}.
 *
 * @author Christoph Strobl
 */
class JpqlQueryBuilderUnitTests {

	@Test // GH-3588
	void placeholdersRenderCorrectly() {

		assertThat(JpqlQueryBuilder.parameter(ParameterPlaceholder.indexed(1)).render(RenderContext.EMPTY)).isEqualTo("?1");
		assertThat(JpqlQueryBuilder.parameter(ParameterPlaceholder.named("arg1")).render(RenderContext.EMPTY))
				.isEqualTo(":arg1");
		assertThat(JpqlQueryBuilder.parameter("?1").render(RenderContext.EMPTY)).isEqualTo("?1");
	}

	@Test // GH-3588
	void placeholdersErrorOnInvalidInput() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> JpqlQueryBuilder.parameter((String) null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> JpqlQueryBuilder.parameter(""));
	}

	@Test // GH-3588
	void stringLiteralRendersAsQuotedString() {

		assertThat(literal("literal").render(RenderContext.EMPTY)).isEqualTo("'literal'");

		/* JPA Spec - 4.6.1 Literals:
		   > A string literal that includes a single quote is represented by two single quotes--for example: 'literal''s'. */
		assertThat(literal("literal's").render(RenderContext.EMPTY)).isEqualTo("'literal''s'");
	}

	@Test // GH-3588
	void entity() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		assertThat(entity.getAlias()).isEqualTo("o");
		assertThat(entity.getEntity()).isEqualTo(Order.class.getName());
		assertThat(entity.getName()).isEqualTo(Order.class.getSimpleName());
	}

	@Test // GH-3588
	void literalExpressionRendersAsIs() {
		Expression expression = expression("CONCAT(person.lastName, ‘, ’, person.firstName))");
		assertThat(expression.render(RenderContext.EMPTY)).isEqualTo("CONCAT(person.lastName, ‘, ’, person.firstName))");
	}

	@Test // GH-3588
	void xxx() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		PathAndOrigin orderDate = JpqlQueryBuilder.path(entity, "date");

		String fragment = JpqlQueryBuilder.where(orderDate).eq(expression("{d '2024-11-05'}")).render(ctx(entity));

		assertThat(fragment).isEqualTo("o.date = {d '2024-11-05'}");
	}

	@Test // GH-3588
	void predicateRendering() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		WhereStep where = JpqlQueryBuilder.where(JpqlQueryBuilder.path(entity, "country"));
		RenderContext context = ctx(entity);

		assertThat(where.between(expression("'AT'"), expression("'DE'")).render(context))
				.isEqualTo("o.country BETWEEN 'AT' AND 'DE'");
		assertThat(where.eq(expression("'AT'")).render(context)).isEqualTo("o.country = 'AT'");
		assertThat(where.eq(literal("AT")).render(context)).isEqualTo("o.country = 'AT'");
		assertThat(where.gt(expression("'AT'")).render(context)).isEqualTo("o.country > 'AT'");
		assertThat(where.gte(expression("'AT'")).render(context)).isEqualTo("o.country >= 'AT'");

		// TODO: that is really really bad
		// lange namen
		assertThat(where.in(expression("'AT', 'DE'")).render(context)).isEqualTo("o.country IN ('AT', 'DE')");

		// 1 in age - cleanup what is not used - remove everything eles
		// assertThat(where.inMultivalued("'AT', 'DE'").render(ctx(entity))).isEqualTo("o.country IN ('AT', 'DE')"); //
		assertThat(where.isEmpty().render(context)).isEqualTo("o.country IS EMPTY");
		assertThat(where.isNotEmpty().render(context)).isEqualTo("o.country IS NOT EMPTY");
		assertThat(where.isTrue().render(context)).isEqualTo("o.country = TRUE");
		assertThat(where.isFalse().render(context)).isEqualTo("o.country = FALSE");
		assertThat(where.isNull().render(context)).isEqualTo("o.country IS NULL");
		assertThat(where.isNotNull().render(context)).isEqualTo("o.country IS NOT NULL");
		assertThat(where.like("'\\_%'", "" + EscapeCharacter.DEFAULT.getEscapeCharacter()).render(context))
				.isEqualTo("o.country LIKE '\\_%' ESCAPE '\\'");
		assertThat(where.notLike(expression("'\\_%'"), "" + EscapeCharacter.DEFAULT.getEscapeCharacter()).render(context))
				.isEqualTo("o.country NOT LIKE '\\_%' ESCAPE '\\'");
		assertThat(where.lt(expression("'AT'")).render(context)).isEqualTo("o.country < 'AT'");
		assertThat(where.lte(expression("'AT'")).render(context)).isEqualTo("o.country <= 'AT'");
		assertThat(where.memberOf(expression("'AT'")).render(context)).isEqualTo("'AT' MEMBER OF o.country");
		// TODO: can we have this where.value(foo).memberOf(pathAndOrigin);
		assertThat(where.notMemberOf(expression("'AT'")).render(context)).isEqualTo("'AT' NOT MEMBER OF o.country");
		assertThat(where.neq(expression("'AT'")).render(context)).isEqualTo("o.country != 'AT'");
	}

	@Test // GH-3588
	void selectRendering() {

		// make sure things are immutable
		SelectStep select = JpqlQueryBuilder.selectFrom(Order.class); // the select step is mutable - not sure i like it
		// hm, I somehow exepect this to render only the selection part
		assertThat(select.count().render()).startsWith("SELECT COUNT(o)");
		assertThat(select.distinct().entity().render()).startsWith("SELECT DISTINCT o ");
		assertThat(select.distinct().count().render()).startsWith("SELECT COUNT(DISTINCT o) ");
		assertThat(JpqlQueryBuilder.selectFrom(Order.class)
				.select(JpqlQueryBuilder.path(JpqlQueryBuilder.entity(Order.class), "country")).render())
				.startsWith("SELECT o.country ");
	}

	@Test // GH-3588
	void joins() {

		Entity entity = JpqlQueryBuilder.entity(LineItem.class);
		Join li_pr = JpqlQueryBuilder.innerJoin(entity, "product");
		Join li_pr2 = JpqlQueryBuilder.innerJoin(entity, "product2");

		PathAndOrigin productName = JpqlQueryBuilder.path(li_pr, "name");
		PathAndOrigin personName = JpqlQueryBuilder.path(li_pr2, "name");

		String fragment = JpqlQueryBuilder.where(productName).eq(literal("ex30"))
				.and(JpqlQueryBuilder.where(personName).eq(literal("ex40"))).render(ctx(entity));

		assertThat(fragment).isEqualTo("p.name = 'ex30' AND join_0.name = 'ex40'");
	}

	@Test // GH-3588
	void joinOnPaths() {

		Entity entity = JpqlQueryBuilder.entity(LineItem.class);
		Join li_pr = JpqlQueryBuilder.innerJoin(entity, "product");
		Join li_pe = JpqlQueryBuilder.innerJoin(entity, "person");

		PathAndOrigin productName = JpqlQueryBuilder.path(li_pr, "name");
		PathAndOrigin personName = JpqlQueryBuilder.path(li_pe, "name");

		String fragment = JpqlQueryBuilder.where(productName).eq(literal("ex30"))
				.and(JpqlQueryBuilder.where(personName).eq(literal("cstrobl"))).render(ctx(entity));

		assertThat(fragment).isEqualTo("p.name = 'ex30' AND join_0.name = 'cstrobl'");
	}

	static RenderContext ctx(Entity... entities) {

		Map<Origin, String> aliases = new LinkedHashMap<>(entities.length);
		for (Entity entity : entities) {
			aliases.put(entity, entity.getAlias());
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
