/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Representation of a query criteria node and its relevant joins.
 * 
 * @author Greg Turnquist
 */
interface ExpressionContext {

	String joins(String alias);

	String criteria(String alias);

	/**
	 * Combine all joins from a list of {@link ExpressionContext}s and filter out duplicates.
	 * 
	 * @param alias
	 * @param expressions
	 * @return
	 */
	default String uniqueJoins(String alias, List<ExpressionContext> expressions) {

		Set<String> uniqueJoins = expressions.stream() //
				.map(expressionContext -> expressionContext.joins(alias)) //
				.collect(Collectors.toSet());

		return String.join("", uniqueJoins);
	}

	default ExpressionContext get(String segment) {
		return this;
	}

	default ExpressionContext get(String path, ManagedType<?> entityType, Class<?> domainClass,
			SingularAttribute<?, ?> attribute) {
		return this;
	}

	default ExpressionContext join(String name) {
		return this;
	}

	default ExpressionContext join(Join join) {
		return this;
	}

	default List<Join> joins() {
		return List.of();
	}

	// Operators

	static ExpressionContext between(ExpressionContext expression, String value1, String value2) {
		return new BetweenExpressionContext(expression, value1, value2);
	}

	static ExpressionContext isNull(ExpressionContext expression) {
		return new IsNullExpressionContext(expression);
	}

	static ExpressionContext isNotNull(ExpressionContext expression) {
		return new IsNotNullExpressionContext(expression);
	}

	static ExpressionContext upper(ExpressionContext expression) {
		return new UpperExpressionContext(expression);
	}

	static ExpressionContext notIn(ExpressionContext expression, String value) {
		return new NotInExpressionContext(expression, value);
	}

	static ExpressionContext in(ExpressionContext expression, String value) {
		return new InExpressionContext(expression, value);
	}

	static ExpressionContext notLike(ExpressionContext propertyExpression, ExpressionContext parameterExpression) {
		return new NotLikeExpressionContext(propertyExpression, parameterExpression);
	}

	static ExpressionContext like(ExpressionContext propertyExpression, ExpressionContext parameterExpression) {
		return new LikeExpressionContext(propertyExpression, parameterExpression);
	}

	static ExpressionContext like(ExpressionContext property, String value) {
		return new LikeExpressionContext(property, new ValueBasedExpressionContext(value));
	}

	static ExpressionContext isTrue(ExpressionContext expression) {
		return new IsTrueExpressionContext(expression);
	}

	static ExpressionContext isFalse(ExpressionContext expression) {
		return new IsFalseExpressionContext(expression);
	}

	static ExpressionContext isNotMember(ParameterMetadataContextProvider.ParameterImpl<Object> parameterExpression,
			String propertyExpression) {
		return new NotMemberOfExpressionContext(parameterExpression, propertyExpression);
	}

	static ExpressionContext isMember(ParameterMetadataContextProvider.ParameterImpl<Object> parameterExpression,
			String propertyExpression) {
		return new MemberOfExpressionContext(parameterExpression, propertyExpression);
	}

	static ExpressionContext isNotEmpty(String path) {
		return new IsNotEmptyExpressionContext(path);
	}

	static ExpressionContext isEmpty(String path) {
		return new IsEmptyExpressionContext(path);
	}

	static ExpressionContext comparison(ExpressionContext expression, String comparator, String value) {
		return new ComparisonExpressionContext(expression, comparator, value);
	}

	static ExpressionContext equal(ExpressionContext left, ExpressionContext right) {
		return new EqualsExpressionContext(left, right);
	}

	static ExpressionContext equal(ExpressionContext predicate, Object value) {
		return new EqualsExpressionContext(predicate, new ValueBasedExpressionContext(value));
	}

	static ExpressionContext notEqual(ExpressionContext left, ExpressionContext right) {
		return new NotEqualExpressionContext(left, right);
	}

	// Joins

	interface Join {
		String join();

		String build(String alias);
	}

	record InnerJoin(String join) implements Join {

		@Override
		public String build(String alias) {
			return String.format("join %s.%s as %s", alias, join, join);
		}
	}

	record OuterJoin(String join) implements Join {
		@Override
		public String build(String alias) {
			return String.format("left outer join %s.%s as %s", alias, join, join);
		}
	}

}
