/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.beans.BeanWrapper;
import org.springframework.data.jpa.repository.support.QueryDslJpaQueryContext;
import org.springframework.data.jpa.repository.support.QueryDslJpaUpdateContext;
import org.springframework.data.jpa.repository.support.QueryDslQueryContext;
import org.springframework.data.repository.SoftDelete;
import org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor;
import org.springframework.data.repository.augment.QueryContext.QueryMode;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.PathBuilder;

/**
 * Querydsl implementation of {@link AbstractSoftDeleteQueryAugmentor} to transparently turn delete calls into entity
 * updates and filter queries accordingly.
 * 
 * @author Dev Naruka
 */
public class QueryDslSoftDeleteQueryAugmentor extends
		AbstractSoftDeleteQueryAugmentor<QueryDslJpaQueryContext<?>, QueryDslQueryContext, QueryDslJpaUpdateContext<?>> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor#prepareNativeQuery(org.springframework.data.repository.augment.QueryContext, java.lang.annotation.Annotation)
	 */
	@Override
	protected QueryDslQueryContext prepareNativeQuery(QueryDslQueryContext context, SoftDelete expression) {

		if (!context.getMode().in(QueryMode.FIND)) {
			return context;
		}

		String string = context.getQueryString();
		// TODO: Augment query;

		return context.withQuery(string);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AnnotationBasedQueryAugmentor#prepareQuery(org.springframework.data.repository.augment.QueryContext, java.lang.annotation.Annotation)
	 */
	@Override
	protected QueryDslJpaQueryContext<?> prepareQuery(QueryDslJpaQueryContext<?> context, SoftDelete expression) {

		JPQLQuery query = context.getQuery();
		PathBuilder<?> builder = context.getPathBuilder();

		Predicate predicate = builder.get(expression.value()).eq(expression.flagMode().activeValue());
		query.where(predicate);

		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor#updateDeletedState(java.lang.Object, org.springframework.data.repository.augment.UpdateContext)
	 */
	@Override
	public void updateDeletedState(Object entity, QueryDslJpaUpdateContext<?> context) {

		@SuppressWarnings("unchecked")
		QueryDslJpaUpdateContext<Object> castedContext = (QueryDslJpaUpdateContext<Object>) context;
		castedContext.update().set(castedContext.getRoot(), entity);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor#createBeanWrapper(org.springframework.data.repository.augment.UpdateContext)
	 */
	@Override
	protected BeanWrapper createBeanWrapper(QueryDslJpaUpdateContext<?> context) {
		return new PropertyChangeEnsuringBeanWrapper(context.getEntity());
	}
}
