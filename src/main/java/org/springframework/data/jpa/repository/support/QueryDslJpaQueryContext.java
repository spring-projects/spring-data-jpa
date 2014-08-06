/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.jpa.repository.support;

import org.springframework.data.repository.augment.QueryContext;

import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.path.PathBuilder;

/**
 * @author Dev Naruka
 */
public class QueryDslJpaQueryContext<T> extends QueryContext<JPAQuery> {

	private final EntityPath<T> root;
	private final PathBuilder<T> pathBuilder;

	public QueryDslJpaQueryContext(JPAQuery query, EntityPath<T> root, PathBuilder<T> builder, QueryMode queryMode) {
		super(query, queryMode);
		this.root = root;
		this.pathBuilder = builder;
	}

	public EntityPath<T> getRoot() {
		return root;
	}

	public PathBuilder<T> getPathBuilder() {
		return pathBuilder;
	}
}
