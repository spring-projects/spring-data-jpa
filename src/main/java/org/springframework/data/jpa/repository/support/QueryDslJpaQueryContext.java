/**
 * 
 */
package org.springframework.data.jpa.repository.support;

import org.springframework.data.repository.augment.QueryContext;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.path.PathBuilder;

/**
 * @author Dev Naruka
 *
 */
public class QueryDslJpaQueryContext<T> extends QueryContext<JPQLQuery> {

	private final EntityPath<T> root;
	private final PathBuilder<T> pathBuilder;
	
	public QueryDslJpaQueryContext(JPQLQuery query, EntityPath<T> root, PathBuilder<T> builder, QueryMode queryMode) {
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
