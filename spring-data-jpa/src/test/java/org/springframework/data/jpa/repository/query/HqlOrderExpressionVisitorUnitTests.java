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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Selection;

import java.util.Locale;

import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify that {@link JpaSort#unsafe(String...)} works properly with Hibernate via {@link HqlOrderExpressionVisitor}.
 *
 * @author Greg Turnquist
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
class HqlOrderExpressionVisitorUnitTests {

	@PersistenceContext EntityManager em;

	@Test
	void genericFunctions() {

		assertThat(renderOrderBy(JpaSort.unsafe("LENGTH(firstname)"), "var_1"))
				.startsWithIgnoringCase("order by character_length(var_1.firstname) asc");
		assertThat(renderOrderBy(JpaSort.unsafe("char_length(firstname)"), "var_1"))
				.startsWithIgnoringCase("order by char_length(var_1.firstname) asc");

		assertThat(renderOrderBy(JpaSort.unsafe("nlssort(firstname, 'NLS_SORT = XGERMAN_DIN_AI')"), "var_1"))
				.startsWithIgnoringCase("order by nlssort(var_1.firstname, 'NLS_SORT = XGERMAN_DIN_AI')");
	}

	@Test // GH-3172
	void cast() {

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> renderOrderBy(JpaSort.unsafe("cast(emailAddress as date)"), "var_1"));
	}

	@Test // GH-3172
	void extract() {

		assertThat(renderOrderBy(JpaSort.unsafe("EXTRACT(DAY FROM createdAt)"), "var_1"))
				.startsWithIgnoringCase("order by extract(day from var_1.createdAt)");

		assertThat(renderOrderBy(JpaSort.unsafe("WEEK(createdAt)"), "var_1"))
				.startsWithIgnoringCase("order by extract(week from var_1.createdAt)");
	}

	@Test // GH-3172
	void trunc() {
		assertThat(renderOrderBy(JpaSort.unsafe("TRUNC(age)"), "var_1"))
				.startsWithIgnoringCase("order by trunc(var_1.age)");
	}

	@Test // GH-3172
	void upperLower() {
		assertThat(renderOrderBy(JpaSort.unsafe("upper(firstname)"), "var_1"))
				.startsWithIgnoringCase("order by upper(var_1.firstname)");
		assertThat(renderOrderBy(JpaSort.unsafe("lower(firstname)"), "var_1"))
				.startsWithIgnoringCase("order by lower(var_1.firstname)");
	}

	@Test // GH-3172
	void substring() {
		assertThat(renderOrderBy(JpaSort.unsafe("substring(emailAddress, 0, 3)"), "var_1"))
				.startsWithIgnoringCase("order by substring(var_1.emailAddress, 0, 3) asc");
		assertThat(renderOrderBy(JpaSort.unsafe("substring(emailAddress, 0)"), "var_1"))
				.startsWithIgnoringCase("order by substring(var_1.emailAddress, 0) asc");
	}

	@Test // GH-3172
	void repeat() {
		assertThat(renderOrderBy(JpaSort.unsafe("repeat('a', 5)"), "var_1"))
				.startsWithIgnoringCase("order by repeat('a', 5) asc");
	}

	@Test // GH-3172
	void literals() {

		assertThat(renderOrderBy(JpaSort.unsafe("age + 1"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1l"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1L"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1f"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1bi"), "var_1"))
				.startsWithIgnoringCase("order by var_1.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1bd"), "var_1"))
				.startsWithIgnoringCase("order by var_1.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 0x12"), "var_1")).startsWithIgnoringCase("order by var_1.age + 18");
	}

	@Test // GH-3172
	void temporalLiterals() {

		// JDBC
		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts '2024-01-01 12:34:56'}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '2024-01-01T12:34:56'");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts '2012-01-03 09:00:00.000000001'}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '2012-01-03T09:00:00.000000001'");

		// Hibernate NPE
		assertThatIllegalArgumentException()
				.isThrownBy(() -> renderOrderBy(JpaSort.unsafe("createdAt + {t '12:34:56'}"), "var_1"));

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {d '2024-01-01'}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '2024-01-01'");

		// JPQL
		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts 2024-01-01 12:34:56}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '2024-01-01T12:34:56'");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {t 12:34:56}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '12:34:56'");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {d 2024-01-01}"), "var_1"))
				.startsWithIgnoringCase("order by var_1.createdAt + '2024-01-01'");
	}

	@Test // GH-3172
	void arithmetic() {

		// Hibernate representation bugs, should be sum(var_1.age)
		assertThat(renderOrderBy(JpaSort.unsafe("sum(age)"), "var_1")).startsWithIgnoringCase("order by sum()");
		assertThat(renderOrderBy(JpaSort.unsafe("min(age)"), "var_1")).startsWithIgnoringCase("order by min()");
		assertThat(renderOrderBy(JpaSort.unsafe("max(age)"), "var_1")).startsWithIgnoringCase("order by max()");

		assertThat(renderOrderBy(JpaSort.unsafe("age"), "var_1")).startsWithIgnoringCase("order by var_1.age");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1"), "var_1")).startsWithIgnoringCase("order by var_1.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("ABS(age) + 1"), "var_1"))
				.startsWithIgnoringCase("order by abs(var_1.age) + 1");

		assertThat(renderOrderBy(JpaSort.unsafe("neg(active)"), "var_1"))
				.startsWithIgnoringCase("order by neg(var_1.active)");
		assertThat(renderOrderBy(JpaSort.unsafe("abs(age)"), "var_1")).startsWithIgnoringCase("order by abs(var_1.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("ceiling(age)"), "var_1"))
				.startsWithIgnoringCase("order by ceiling(var_1.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("floor(age)"), "var_1"))
				.startsWithIgnoringCase("order by floor(var_1.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("round(age)"), "var_1"))
				.startsWithIgnoringCase("order by round(var_1.age)");

		assertThat(renderOrderBy(JpaSort.unsafe("prod(age, 1)"), "var_1"))
				.startsWithIgnoringCase("order by prod(var_1.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("prod(age, age)"), "var_1"))
				.startsWithIgnoringCase("order by prod(var_1.age, var_1.age)");

		assertThat(renderOrderBy(JpaSort.unsafe("diff(age, 1)"), "var_1"))
				.startsWithIgnoringCase("order by diff(var_1.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("quot(age, 1)"), "var_1"))
				.startsWithIgnoringCase("order by quot(var_1.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("mod(age, 1)"), "var_1"))
				.startsWithIgnoringCase("order by mod(var_1.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("sqrt(age)"), "var_1")).startsWithIgnoringCase("order by sqrt(var_1.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("exp(age)"), "var_1")).startsWithIgnoringCase("order by exp(var_1.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("ln(age)"), "var_1")).startsWithIgnoringCase("order by ln(var_1.age)");
	}

	@Test // GH-3172
	@Disabled("HHH-19075")
	void trim() {
		assertThat(renderOrderBy(JpaSort.unsafe("trim(leading '.' from lastname)"), "var_1"))
				.startsWithIgnoringCase("order by repeat('a', 5) asc");
	}

	@Test // GH-3172
	void groupedExpression() {
		assertThat(renderOrderBy(JpaSort.unsafe("(lastname)"), "var_1")).startsWithIgnoringCase("order by var_1.lastname");
	}

	@Test // GH-3172
	void tupleExpression() {
		assertThat(renderOrderBy(JpaSort.unsafe("(firstname, lastname)"), "var_1"))
				.startsWithIgnoringCase("order by var_1.firstname, var_1.lastname");
	}

	@Test // GH-3172
	void concat() {
		assertThat(renderOrderBy(JpaSort.unsafe("firstname || lastname"), "var_1"))
				.startsWithIgnoringCase("order by concat(var_1.firstname, var_1.lastname)");
	}

	@Test // GH-3172
	void pathBased() {

		String query = renderQuery(JpaSort.unsafe("manager.firstname"), "var_1");

		assertThat(query).contains("from org.springframework.data.jpa.domain.sample.User var_1 left join var_1.manager");
		assertThat(query).contains(".firstname asc nulls last");
	}

	@Test // GH-3172
	void caseSwitch() {

		assertThat(renderOrderBy(JpaSort.unsafe("case firstname when 'Oliver' then 'A' else firstname end"), "var_1"))
				.startsWithIgnoringCase("order by case var_1.firstname when 'Oliver' then 'A' else var_1.firstname end");

		assertThat(renderOrderBy(
				JpaSort.unsafe("case firstname when 'Oliver' then 'A' when 'Joachim' then 'z' else firstname end"), "var_1"))
				.startsWithIgnoringCase(
						"order by case var_1.firstname when 'Oliver' then 'A' when 'Joachim' then 'z' else var_1.firstname end");

		assertThat(renderOrderBy(JpaSort.unsafe("case when age < 31 then 'A' else firstname end"), "var_1"))
				.startsWithIgnoringCase("order by case when var_1.age < 31 then 'A' else var_1.firstname end");

		assertThat(
				renderOrderBy(JpaSort.unsafe("case when firstname not in ('Oliver', 'Dave') then 'A' else firstname end"),
						"var_1"))
				.startsWithIgnoringCase(
						"order by case when var_1.firstname not in ('Oliver', 'Dave') then 'A' else var_1.firstname end");
	}

	private String renderOrderBy(JpaSort sort, String alias) {

		String query = renderQuery(sort, alias);
		String lowerCase = query.toLowerCase(Locale.ROOT);
		int index = lowerCase.indexOf("order by");

		if (index != -1) {
			return query.substring(index);
		}

		return "";
	}

	CriteriaQuery<User> createQuery(JpaSort sort, String alias) {

		CriteriaQuery<User> query = em.getCriteriaBuilder().createQuery(User.class);
		Selection<User> from = query.from(User.class).alias(alias);
		HqlOrderExpressionVisitor extractor = new HqlOrderExpressionVisitor(em.getCriteriaBuilder(), (Path<?>) from,
				QueryUtils::toExpressionRecursively);

		Expression<?> expression = extractor.createCriteriaExpression(sort.stream().findFirst().get());
		return query.select(from).orderBy(em.getCriteriaBuilder().asc(expression, Nulls.NONE));
	}

	@SuppressWarnings("rawtypes")
	String renderQuery(JpaSort sort, String alias) {

		CriteriaQuery<User> q = createQuery(sort, alias);
		SqmSelectStatement s = (SqmSelectStatement) q;

		StringBuilder builder = new StringBuilder();
		s.appendHqlString(builder, SqmRenderContext.simpleContext());

		return builder.toString();
	}
}
