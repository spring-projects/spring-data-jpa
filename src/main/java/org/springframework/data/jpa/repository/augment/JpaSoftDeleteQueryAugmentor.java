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
package org.springframework.data.jpa.repository.augment;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.repository.support.JpaCriteriaQueryContext;
import org.springframework.data.jpa.repository.support.JpaQueryContext;
import org.springframework.data.jpa.repository.support.JpaUpdateContext;
import org.springframework.data.repository.SoftDelete;
import org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor;
import org.springframework.data.repository.augment.QueryContext.QueryMode;

/**
 * JPA implementation of {@link AbstractSoftDeleteQueryAugmentor} to transparently turn delete calls into entity
 * updates. Also filters queries accordingly.
 * 
 * @author Oliver Gierke
 */
public class JpaSoftDeleteQueryAugmentor extends
		AbstractSoftDeleteQueryAugmentor<JpaCriteriaQueryContext<?, ?>, JpaQueryContext, JpaUpdateContext<?>> {

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor#prepareNativeQuery(org.springframework.data.repository.augment.QueryContext, java.lang.annotation.Annotation)
	 */
	@Override
	protected JpaQueryContext prepareNativeQuery(JpaQueryContext context, SoftDelete expression) {

		if (!context.getMode().in(QueryMode.FIND)) {
			return context;
		}

		String string = context.getQueryString();
		// TODO: Augment query;

		return context.withQuery(string);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.sample.JpaSoftDeleteQueryAugmentor#prepareQuery(org.springframework.data.jpa.repository.support.JpaCriteriaQueryContext, org.springframework.data.jpa.repository.support.GlobalFilter)
	 */
	@Override
	protected JpaCriteriaQueryContext<?, ?> prepareQuery(JpaCriteriaQueryContext<?, ?> context, SoftDelete expression) {

		CriteriaQuery<?> criteriaQuery = context.getQuery();
		CriteriaBuilder builder = context.getCriteriaBuilder();

		Predicate predicate = builder.equal(context.getRoot().get(expression.value()), expression.flagMode().activeValue());
		Predicate restriction = criteriaQuery.getRestriction();

		criteriaQuery.where(restriction == null ? predicate : builder.and(restriction, predicate));

		return context;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor#updateDeletedState(java.lang.Object)
	 */
	@Override
	public void updateDeletedState(Object entity, JpaUpdateContext<?> context) {
		context.getEntityManager().merge(entity);
	}
}
