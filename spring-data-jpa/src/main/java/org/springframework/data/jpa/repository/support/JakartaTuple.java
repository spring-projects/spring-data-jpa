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
package org.springframework.data.jpa.repository.support;

import jakarta.persistence.Tuple;

import java.util.Arrays;
import java.util.List;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionBase;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.FactoryExpression;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.Visitor;
import com.querydsl.jpa.JPQLSerializer;
import org.jspecify.annotations.Nullable;

/**
 * Expression based on a {@link Tuple}. It's a simplified variant of {@link com.querydsl.core.types.QTuple} without
 * being a {@link com.querydsl.core.types.FactoryExpressionBase} as we do not want Querydsl to instantiate any tuples.
 * JPA is doing that for us.
 *
 * @author Mark Paluch
 * @since 3.5
 */
@SuppressWarnings("unchecked")
class JakartaTuple extends ExpressionBase<Tuple> {

	private final List<Expression<?>> args;

	/**
	 * Create a new JakartaTuple instance
	 *
	 * @param args
	 */
	protected JakartaTuple(Expression<?>... args) {
		this(Arrays.asList(args));
	}

	/**
	 * Create a new JakartaTuple instance
	 *
	 * @param args
	 */
	protected JakartaTuple(List<Expression<?>> args) {
		super(Tuple.class);
		this.args = args.stream().map(it -> {

			if (it instanceof Path<?> p) {
				return ExpressionUtils.operation(p.getType(), Ops.ALIAS, p, p);
			}

			return it;
		}).toList();
	}

	@Override
	public <R, C> @Nullable R accept(Visitor<R, C> v, @Nullable C context) {

		if (v instanceof JPQLSerializer) {
			return Projections.tuple(args).accept(v, context);
		}

		return (R) this;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof FactoryExpression<?> c) {
			return args.equals(c.getArgs()) && getType().equals(c.getType());
		} else {
			return false;
		}
	}

}
