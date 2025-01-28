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

		assertThat(renderOrderBy(JpaSort.unsafe("LENGTH(firstname)"), "u"))
				.startsWithIgnoringCase("order by character_length(u.firstname) asc");
		assertThat(renderOrderBy(JpaSort.unsafe("char_length(firstname)"), "u"))
				.startsWithIgnoringCase("order by char_length(u.firstname) asc");

		assertThat(renderOrderBy(JpaSort.unsafe("nlssort(firstname, 'NLS_SORT = XGERMAN_DIN_AI')"), "u"))
				.startsWithIgnoringCase("order by nlssort(u.firstname, 'NLS_SORT = XGERMAN_DIN_AI')");
	}

	@Test // GH-3172
	void cast() {

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> renderOrderBy(JpaSort.unsafe("cast(emailAddress as date)"), "u"));
	}

	@Test // GH-3172
	void extract() {

		assertThat(renderOrderBy(JpaSort.unsafe("EXTRACT(DAY FROM createdAt)"), "u"))
				.startsWithIgnoringCase("order by extract(day from u.createdAt)");

		assertThat(renderOrderBy(JpaSort.unsafe("WEEK(createdAt)"), "u"))
				.startsWithIgnoringCase("order by extract(week from u.createdAt)");
	}

	@Test // GH-3172
	void trunc() {
		assertThat(renderOrderBy(JpaSort.unsafe("TRUNC(age)"), "u")).startsWithIgnoringCase("order by trunc(u.age)");
	}

	@Test // GH-3172
	void upperLower() {
		assertThat(renderOrderBy(JpaSort.unsafe("upper(firstname)"), "u"))
				.startsWithIgnoringCase("order by upper(u.firstname)");
		assertThat(renderOrderBy(JpaSort.unsafe("lower(firstname)"), "u"))
				.startsWithIgnoringCase("order by lower(u.firstname)");
	}

	@Test // GH-3172
	void substring() {
		assertThat(renderOrderBy(JpaSort.unsafe("substring(emailAddress, 0, 3)"), "u"))
				.startsWithIgnoringCase("order by substring(u.emailAddress, 0, 3) asc");
		assertThat(renderOrderBy(JpaSort.unsafe("substring(emailAddress, 0)"), "u"))
				.startsWithIgnoringCase("order by substring(u.emailAddress, 0) asc");
	}

	@Test // GH-3172
	void repeat() {
		assertThat(renderOrderBy(JpaSort.unsafe("repeat('a', 5)"), "u"))
				.startsWithIgnoringCase("order by repeat('a', 5) asc");
	}

	@Test // GH-3172
	void literals() {

		assertThat(renderOrderBy(JpaSort.unsafe("age + 1"), "u")).startsWithIgnoringCase("order by u.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1l"), "u")).startsWithIgnoringCase("order by u.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1L"), "u")).startsWithIgnoringCase("order by u.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1"), "u")).startsWithIgnoringCase("order by u.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1f"), "u")).startsWithIgnoringCase("order by u.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1bi"), "u")).startsWithIgnoringCase("order by u.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1.1bd"), "u")).startsWithIgnoringCase("order by u.age + 1.1");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 0x12"), "u")).startsWithIgnoringCase("order by u.age + 18");
	}

	@Test // GH-3172
	void temporalLiterals() {

		// JDBC
		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts '2024-01-01 12:34:56'}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 2024-01-01T12:34:56");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts '2012-01-03 09:00:00.000000001'}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 2012-01-03T09:00:00.000000001");

		// Hibernate NPE
		assertThatNullPointerException().isThrownBy(() -> renderOrderBy(JpaSort.unsafe("createdAt + {t '12:34:56'}"), "u"));

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {d '2024-01-01'}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 2024-01-01");

		// JPQL
		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {ts 2024-01-01 12:34:56}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 2024-01-01T12:34:56");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {t 12:34:56}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 12:34:56");

		assertThat(renderOrderBy(JpaSort.unsafe("createdAt + {d 2024-01-01}"), "u"))
				.startsWithIgnoringCase("order by u.createdAt + 2024-01-01");
	}

	@Test // GH-3172
	void arithmetic() {

		// Hibernate representation bugs, should be sum(u.age)
		assertThat(renderOrderBy(JpaSort.unsafe("sum(age)"), "u")).startsWithIgnoringCase("order by sum()");
		assertThat(renderOrderBy(JpaSort.unsafe("min(age)"), "u")).startsWithIgnoringCase("order by min()");
		assertThat(renderOrderBy(JpaSort.unsafe("max(age)"), "u")).startsWithIgnoringCase("order by max()");

		assertThat(renderOrderBy(JpaSort.unsafe("age"), "u")).startsWithIgnoringCase("order by u.age");
		assertThat(renderOrderBy(JpaSort.unsafe("age + 1"), "u")).startsWithIgnoringCase("order by u.age + 1");
		assertThat(renderOrderBy(JpaSort.unsafe("ABS(age) + 1"), "u")).startsWithIgnoringCase("order by abs(u.age) + 1");

		assertThat(renderOrderBy(JpaSort.unsafe("neg(active)"), "u")).startsWithIgnoringCase("order by neg(u.active)");
		assertThat(renderOrderBy(JpaSort.unsafe("abs(age)"), "u")).startsWithIgnoringCase("order by abs(u.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("ceiling(age)"), "u")).startsWithIgnoringCase("order by ceiling(u.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("floor(age)"), "u")).startsWithIgnoringCase("order by floor(u.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("round(age)"), "u")).startsWithIgnoringCase("order by round(u.age)");

		assertThat(renderOrderBy(JpaSort.unsafe("prod(age, 1)"), "u")).startsWithIgnoringCase("order by prod(u.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("prod(age, age)"), "u"))
				.startsWithIgnoringCase("order by prod(u.age, u.age)");

		assertThat(renderOrderBy(JpaSort.unsafe("diff(age, 1)"), "u")).startsWithIgnoringCase("order by diff(u.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("quot(age, 1)"), "u")).startsWithIgnoringCase("order by quot(u.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("mod(age, 1)"), "u")).startsWithIgnoringCase("order by mod(u.age, 1)");
		assertThat(renderOrderBy(JpaSort.unsafe("sqrt(age)"), "u")).startsWithIgnoringCase("order by sqrt(u.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("exp(age)"), "u")).startsWithIgnoringCase("order by exp(u.age)");
		assertThat(renderOrderBy(JpaSort.unsafe("ln(age)"), "u")).startsWithIgnoringCase("order by ln(u.age)");
	}

	@Test // GH-3172
	@Disabled("HHH-19075")
	void trim() {
		assertThat(renderOrderBy(JpaSort.unsafe("trim(leading '.' from lastname)"), "u"))
				.startsWithIgnoringCase("order by repeat('a', 5) asc");
	}

	@Test // GH-3172
	void groupedExpression() {
		assertThat(renderOrderBy(JpaSort.unsafe("(lastname)"), "u")).startsWithIgnoringCase("order by u.lastname");
	}

	@Test // GH-3172
	void tupleExpression() {
		assertThat(renderOrderBy(JpaSort.unsafe("(firstname, lastname)"), "u"))
				.startsWithIgnoringCase("order by u.firstname, u.lastname");
	}

	@Test // GH-3172
	void concat() {
		assertThat(renderOrderBy(JpaSort.unsafe("firstname || lastname"), "u"))
				.startsWithIgnoringCase("order by concat(u.firstname, u.lastname)");
	}

	@Test // GH-3172
	void pathBased() {

		String query = renderQuery(JpaSort.unsafe("manager.firstname"), "u");

		assertThat(query).contains("from org.springframework.data.jpa.domain.sample.User u left join u.manager");
		assertThat(query).contains(".firstname asc nulls last");
	}

	@Test // GH-3172
	void caseSwitch() {

		assertThat(renderOrderBy(JpaSort.unsafe("case firstname when 'Oliver' then 'A' else firstname end"), "u"))
				.startsWithIgnoringCase("order by case u.firstname when 'Oliver' then 'A' else u.firstname end");

		assertThat(renderOrderBy(
				JpaSort.unsafe("case firstname when 'Oliver' then 'A' when 'Joachim' then 'z' else firstname end"), "u"))
				.startsWithIgnoringCase(
						"order by case u.firstname when 'Oliver' then 'A' when 'Joachim' then 'z' else u.firstname end");

		assertThat(renderOrderBy(JpaSort.unsafe("case when age < 31 then 'A' else firstname end"), "u"))
				.startsWithIgnoringCase("order by case when u.age < 31 then 'A' else u.firstname end");

		assertThat(
				renderOrderBy(JpaSort.unsafe("case when firstname not in ('Oliver', 'Dave') then 'A' else firstname end"), "u"))
				.startsWithIgnoringCase(
						"order by case when u.firstname not in ('Oliver', 'Dave') then 'A' else u.firstname end");
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
		s.appendHqlString(builder);

		return builder.toString();
	}
}
