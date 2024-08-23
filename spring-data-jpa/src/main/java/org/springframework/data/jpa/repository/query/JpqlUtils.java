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

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.util.Objects;

import org.springframework.data.mapping.PropertyPath;

/**
 * @author Mark Paluch
 */
class JpqlUtils {

	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(JpqlQueryBuilder.Origin source, From<?, ?> from,
			PropertyPath property) {
		return toExpressionRecursively(source, from, property, false);
	}

	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(JpqlQueryBuilder.Origin source, From<?, ?> from,
			PropertyPath property, boolean isForSelection) {
		return toExpressionRecursively(source, from, property, isForSelection, false);
	}

	/**
	 * Creates an expression with proper inner and left joins by recursively navigating the path
	 *
	 * @param from the {@link From}
	 * @param property the property path
	 * @param isForSelection is the property navigated for the selection or ordering part of the query?
	 * @param hasRequiredOuterJoin has a parent already required an outer join?
	 * @param <T> the type of the expression
	 * @return the expression
	 */
	@SuppressWarnings("unchecked")
	static JpqlQueryBuilder.PathAndOrigin toExpressionRecursively(JpqlQueryBuilder.Origin source, From<?, ?> from,
			PropertyPath property, boolean isForSelection, boolean hasRequiredOuterJoin) {

		String segment = property.getSegment();

		boolean isLeafProperty = !property.hasNext();

		boolean requiresOuterJoin = QueryUtils.requiresOuterJoin(from, property, isForSelection, hasRequiredOuterJoin);

		// if it does not require an outer join and is a leaf, simply get the segment
		if (!requiresOuterJoin && isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, source, false);
		}

		// get or create the join
		JpqlQueryBuilder.Join joinSource = requiresOuterJoin ? JpqlQueryBuilder.leftJoin(source, segment)
				: JpqlQueryBuilder.innerJoin(source, segment);
		JoinType joinType = requiresOuterJoin ? JoinType.LEFT : JoinType.INNER;
		Join<?, ?> join = QueryUtils.getOrCreateJoin(from, segment, joinType);

		// if it's a leaf, return the join
		if (isLeafProperty) {
			return new JpqlQueryBuilder.PathAndOrigin(property, joinSource, true);
		}

		PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null");

		// recurse with the next property
		return toExpressionRecursively(joinSource, join, nextProperty, isForSelection, requiresOuterJoin);
	}
}
