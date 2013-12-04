/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Sort option for queries that wraps JPA MetaModel {@link Expression}s for sorting.
 * 
 * @author Thomas Darimont
 */
public class JpaSort extends Sort {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new {@link JpaSort} instance with the given {@link Path}s.
	 * 
	 * @param jpaPaths must not be {@literal null} or empty.
	 */
	public JpaSort(Path<?>... jpaPaths) {
		this(Arrays.asList(jpaPaths));
	}

	/**
	 * Creates a new {@link JpaSort} instance with the given {@link Path}s.
	 * 
	 * @param direction
	 * @param jpaPaths must not be {@literal null} or empty.
	 */
	public JpaSort(Direction direction, Path<?>... jpaPaths) {
		this(direction, Arrays.asList(jpaPaths));
	}

	/**
	 * Creates a new {@link JpaSort} instance with the given {@link Path}s.
	 * 
	 * @param jpaPaths must not be {@literal null} or empty.
	 */
	public JpaSort(List<Path<?>> jpaPaths) {
		this(DEFAULT_DIRECTION, jpaPaths);
	}

	/**
	 * Creates a new {@link JpaSort} instance with the given {@link Path}s.
	 * 
	 * @param direction
	 * @param jpaPaths must not be {@literal null} or empty.
	 */
	public JpaSort(Direction direction, List<Path<?>> jpaPaths) {
		super(direction, toPropertyPaths(jpaPaths));
	}

	/**
	 * @param jpaPaths must not be {@literal null} or empty.
	 * @return
	 */
	private static List<String> toPropertyPaths(List<Path<?>> jpaPaths) {

		Assert.notEmpty(jpaPaths, "Jpa orders must not be null or empty!");

		List<String> propertyPaths = new ArrayList<String>();

		for (Path<?> path : jpaPaths) {
			propertyPaths.add(toPropertyPath(path));
		}

		return propertyPaths;
	}

	/**
	 * @param path
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static String toPropertyPath(Path path) {

		StringBuilder attributePath = new StringBuilder();
		Path current = path;
		while (!(current instanceof Root)) {
			String attributePathSegment = ((SingularAttribute) current.getModel()).getName();
			if (attributePath.length() > 0) {
				attributePath.insert(0, ".");
			}
			attributePath.insert(0, attributePathSegment);
			current = current.getParentPath();
		}

		return attributePath.toString();
	}
}
