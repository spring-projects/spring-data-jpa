package org.springframework.data.jpa.repository.sample;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.DeclaredQuery;
import org.springframework.data.jpa.repository.query.QueryEnhancer;
import org.springframework.data.jpa.repository.query.QueryUtils;

public class MyCustomQueryEnhancer extends QueryEnhancer {

	public MyCustomQueryEnhancer(DeclaredQuery query) {
		super(query);
	}

	@Override
	public String applySorting(Sort sort, String alias) {
		return QueryUtils.applySorting(getQuery().getQueryString(), sort, alias);
	}

	@Override
	public String detectAlias() {
		return QueryUtils.detectAlias(getQuery().getQueryString());
	}

	@Override
	public String createCountQueryFor(String countProjection) {
		// we return this because we use this to test if the correct enhancer is used
		return "Select distinct(1) From User u";
	}

	@Override
	public String getProjection() {
		return QueryUtils.getProjection(getQuery().getQueryString());
	}

	@Override
	public Set<String> getJoinAliases() {
		return QueryUtils.getOuterJoinAliases(this.getQuery().getQueryString());
	}
}
