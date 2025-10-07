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

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpqlQueryBuilder}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Choi Wang Gyu
 */
class JpqlQueryBuilderUnitTests {

	@Test // GH-3588
	void placeholdersRenderCorrectly() {

		assertThatRendered(JpqlQueryBuilder.parameter(ParameterPlaceholder.indexed(1))).isEqualTo("?1");
		assertThatRendered(JpqlQueryBuilder.parameter(ParameterPlaceholder.named("arg1")))
				.isEqualTo(":arg1");
		assertThatRendered(JpqlQueryBuilder.parameter("?1")).isEqualTo("?1");
	}

	@Test // GH-3588
	void placeholdersErrorOnInvalidInput() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> JpqlQueryBuilder.parameter((String) null));
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> JpqlQueryBuilder.parameter(""));
	}

	@Test // GH-3588
	void stringLiteralRendersAsQuotedString() {

		assertThatRendered(literal("literal")).isEqualTo("'literal'");

		/* JPA Spec - 4.6.1 Literals:
		   > A string literal that includes a single quote is represented by two single quotes--for example: 'literal''s'. */
		assertThatRendered(literal("literal's")).isEqualTo("'literal''s'");
	}

	@Test // GH-3588
	void entity() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);

		assertThat(entity.getAlias()).isEqualTo("o");
		assertThat(entity.getName()).isEqualTo(Order.class.getName());
	}

	@Test // GH-4032
	void considersEntityName() {

		Entity entity = JpqlQueryBuilder.entity(Product.class);

		assertThat(entity.getAlias()).isEqualTo("p");
		assertThat(entity.getName()).isEqualTo("my_product");
	}

	@Test // GH-3588
	void literalExpressionRendersAsIs() {
		Expression expression = expression("CONCAT(person.lastName, ‘, ’, person.firstName))");
		assertThatRendered(expression).isEqualTo("CONCAT(person.lastName, ‘, ’, person.firstName))");
	}

	@Test // GH-3961
	void aliasedExpression() {

		// aliasing is contextual and happens during selection rendering. E.g. constructor expressions don't use aliases.
		Expression expression = expression("CONCAT(person.lastName, ‘, ’, person.firstName)").as("concatted");
		assertThatRendered(expression)
				.isEqualTo("CONCAT(person.lastName, ‘, ’, person.firstName)");
	}

	@Test // GH-3961
	void shouldRenderDateAsJpqlLiteral() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		PathAndOrigin orderDate = JpqlQueryBuilder.path(entity, "date");

		String fragment = JpqlQueryBuilder.where(orderDate).eq(expression("{d '2024-11-05'}")).render(ctx(entity));

		assertThat(fragment).isEqualTo("o.date = {d '2024-11-05'}");
	}

	@Test // GH-3588
	void predicateRendering() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		WhereStep where = JpqlQueryBuilder.where(JpqlQueryBuilder.path(entity, "country"));
		ContextualAssert ctx = contextual(ctx(entity));

		ctx.assertThat(where.between(expression("'AT'"), expression("'DE'")))
				.isEqualTo("o.country BETWEEN 'AT' AND 'DE'");
		ctx.assertThat(where.eq(expression("'AT'"))).isEqualTo("o.country = 'AT'");
		ctx.assertThat(where.eq(literal("AT"))).isEqualTo("o.country = 'AT'");
		ctx.assertThat(where.gt(expression("'AT'"))).isEqualTo("o.country > 'AT'");
		ctx.assertThat(where.gte(expression("'AT'"))).isEqualTo("o.country >= 'AT'");

		ctx.assertThat(where.in(expression("'AT', 'DE'"))).isEqualTo("o.country IN ('AT', 'DE')");

		// 1 in age - cleanup what is not used - remove everything eles
		// assertThat(where.inMultivalued("'AT', 'DE'").render(ctx(entity))).isEqualTo("o.country IN ('AT', 'DE')"); //
		ctx.assertThat(where.isEmpty()).isEqualTo("o.country IS EMPTY");
		ctx.assertThat(where.isNotEmpty()).isEqualTo("o.country IS NOT EMPTY");
		ctx.assertThat(where.isTrue()).isEqualTo("o.country = TRUE");
		ctx.assertThat(where.isFalse()).isEqualTo("o.country = FALSE");
		ctx.assertThat(where.isNull()).isEqualTo("o.country IS NULL");
		ctx.assertThat(where.isNotNull()).isEqualTo("o.country IS NOT NULL");
		ctx.assertThat(where.like("'\\_%'", "" + EscapeCharacter.DEFAULT.getEscapeCharacter()))
				.isEqualTo("o.country LIKE '\\_%' ESCAPE '\\'");
		ctx.assertThat(where.notLike(expression("'\\_%'"), "" + EscapeCharacter.DEFAULT.getEscapeCharacter()))
				.isEqualTo("o.country NOT LIKE '\\_%' ESCAPE '\\'");
		ctx.assertThat(where.lt(expression("'AT'"))).isEqualTo("o.country < 'AT'");
		ctx.assertThat(where.lte(expression("'AT'"))).isEqualTo("o.country <= 'AT'");
		ctx.assertThat(where.memberOf(expression("'AT'"))).isEqualTo("'AT' MEMBER OF o.country");

		// TODO: can we have this where.value(foo).memberOf(pathAndOrigin);
		ctx.assertThat(where.notMemberOf(expression("'AT'"))).isEqualTo("'AT' NOT MEMBER OF o.country");
		ctx.assertThat(where.neq(expression("'AT'"))).isEqualTo("o.country != 'AT'");
	}

	@Test // GH-3961
	void inPredicateWithNestedExpression() {

		Entity entity = JpqlQueryBuilder.entity(Order.class);
		WhereStep where = JpqlQueryBuilder.where(JpqlQueryBuilder.path(entity, "country"));
		ContextualAssert ctx = contextual(ctx(entity));

		// Test regular IN predicate with parentheses
		ctx.assertThat(where.in(expression("'AT', 'DE'"))).isEqualTo("o.country IN ('AT', 'DE')");

		// Test IN predicate with already parenthesized expression - should avoid double parentheses
		Expression parenthesizedExpression = expression("('AT', 'DE')");
		ctx.assertThat(where.in(parenthesizedExpression))
				.isEqualTo("o.country IN ('AT', 'DE')");

		// Test NOT IN predicate with already parenthesized expression
		ctx.assertThat(where.notIn(parenthesizedExpression))
				.isEqualTo("o.country NOT IN ('AT', 'DE')");

		// Test IN with subquery (already parenthesized)
		Expression subqueryExpression = expression("(SELECT c.code FROM Country c WHERE c.active = true)");
		ctx.assertThat(where.in(subqueryExpression))
				.isEqualTo("o.country IN (SELECT c.code FROM Country c WHERE c.active = true)");
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

	static ContextualAssert contextual(RenderContext context) {
		return new ContextualAssert(context);
	}

	static AbstractStringAssert<?> assertThatRendered(Renderable renderable) {
		return contextual(RenderContext.EMPTY).assertThat(renderable);
	}

	static AbstractStringAssert<?> assertThat(String actual) {
		return Assertions.assertThat(actual);
	}

	record ContextualAssert(RenderContext context) {

		public AbstractStringAssert<?> assertThat(Renderable renderable) {
			return Assertions.assertThat(renderable.render(context));
		}
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

	@jakarta.persistence.Entity(name = "my_product")
	static class Product {

		@Id Long id;

		String name;
		String productType;

	}
}
