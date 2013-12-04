/*
 * Copyright 2011-2013 the original author or authors.
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
package org.springframework.data.jpa.support;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;

import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.util.Assert;

/**
 * Builder for building JPA meta-model access {@link Path}s that can be used as Path expressions e.g. in {@link JpaSort}
 * 
 * @author Thomas Darimont
 */
public class JpaMetaModelPathBuilder<T, A> {

	private final Class<?> rootType;
	private final List<SingularAttribute<?, ?>> currentPathSegments;

	/**
	 * Static factory method to create a {@link JpaMetaModelPathBuilder} from the given root {@link SingularAttribute}.
	 * 
	 * @param attribute the root attribute to use must not be {@literal null}.
	 * @return
	 */
	public static <T, A> JpaMetaModelPathBuilder<T, A> path(SingularAttribute<T, A> attribute) {
		return new JpaMetaModelPathBuilder<T, A>(attribute.getDeclaringType().getJavaType(), attribute);
	}

	/**
	 * Creates a new {@link JpaMetaModelPathBuilder}.
	 * 
	 * @param rootType the root type of the expression must not be {@literal null}.
	 * @param attribute must not be {@literal null}.
	 */
	private JpaMetaModelPathBuilder(Class<?> rootType, SingularAttribute<T, A> attribute) {
		this(rootType, attribute, new ArrayList<SingularAttribute<?, ?>>());
	}

	/**
	 * Creates a new {@link JpaMetaModelPathBuilder}.
	 * 
	 * @param rootType the root type of the expression must not be {@literal null}.
	 * @param attribute must not be {@literal null}
	 * @param pathSegments must not be {@literal null}
	 */
	private JpaMetaModelPathBuilder(Class<?> rootType, SingularAttribute<T, A> attribute,
			List<SingularAttribute<?, ?>> pathSegments) {

		Assert.notNull(rootType, "Root type must not be null!");
		Assert.notNull(attribute, "Attribute must not be null!");
		Assert.notNull(pathSegments, "Path segments must not be null!");

		this.rootType = rootType;
		this.currentPathSegments = pathSegments;
		this.currentPathSegments.add(attribute);
	}

	/**
	 * Returns a new {@link JpaMetaModelPathBuilder} instance with the given nested {@link SingularAttribute} attached.
	 * 
	 * @param nestedAttribute must not be {@literal null}.
	 * @return
	 */
	public <NA> JpaMetaModelPathBuilder<A, NA> get(SingularAttribute<A, NA> nestedAttribute) {
		return new JpaMetaModelPathBuilder<A, NA>(rootType, nestedAttribute, new ArrayList<SingularAttribute<?, ?>>(
				currentPathSegments));
	}

	/**
	 * Constructs a {@link Path} from the collected {@link SingularAttribute}s.
	 * 
	 * @param em must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Path<A> build(EntityManager em) {

		Assert.notNull(em, "EntityManager must nut be null");

		Root<?> root = em.getCriteriaBuilder().createQuery().from(rootType);

		@SuppressWarnings("rawtypes")
		Path path = root;
		for (SingularAttribute<?, ?> attribute : currentPathSegments) {
			path = path.get(attribute);
		}

		return (Path<A>) path;
	}
}
