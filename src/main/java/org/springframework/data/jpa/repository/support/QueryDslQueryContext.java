/**
 * 
 */
package org.springframework.data.jpa.repository.support;

import org.springframework.data.repository.augment.QueryContext;

import com.mysema.query.jpa.JPQLQuery;

/**
 * @author Dev Naruka
 *
 */
public class QueryDslQueryContext extends QueryContext<JPQLQuery> {

	public QueryDslQueryContext(
			JPQLQuery query,
			QueryMode queryMode) {
		super(query, queryMode);
		// TODO Auto-generated constructor stub
	}

	public String getQueryString() {
		// TODO Auto-generated method stub
		return null;
	}

	public QueryDslQueryContext withQuery(String string) {
		// TODO Auto-generated method stub
		return null;
	}

}
