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

import com.mysema.query.jpa.JPQLQuery;

/**
 * @author Dev Naruka
 */
// TODO Which native implementation to be used? (SQLQuery or JPASQLQuery)
// TODO Where QueryDslQueryContext can be used? (in QueryDslRepositorySupport ???)
public class QueryDslQueryContext extends QueryContext<JPQLQuery> {

	public QueryDslQueryContext(JPQLQuery query, QueryMode queryMode) {
		super(query, queryMode);
	}

	public String getQueryString() {
		return null;
	}

	public QueryDslQueryContext withQuery(String string) {
		return null;
	}
}
