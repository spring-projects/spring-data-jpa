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

import java.lang.reflect.Method;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.BeanWrapper;
import org.springframework.data.jpa.repository.support.JpaCriteriaQueryContext;
import org.springframework.data.jpa.repository.support.JpaQueryContext;
import org.springframework.data.jpa.repository.support.JpaUpdateContext;
import org.springframework.data.repository.SoftDelete;
import org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor;
import org.springframework.data.repository.augment.QueryContext.QueryMode;
import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.ReflectionUtils;

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

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.AbstractSoftDeleteQueryAugmentor#prepareBeanWrapper(org.springframework.data.repository.augment.UpdateContext)
	 */
	@Override
	protected BeanWrapper createBeanWrapper(JpaUpdateContext<?> context) {
		return new PropertyChangeEnsuringBeanWrapper(context.getEntity());
	}

	/**
	 * Custom {@link DirectFieldAccessFallbackBeanWrapper} to hook in additional functionality when setting a property by
	 * field access.
	 * 
	 * @author Oliver Gierke
	 */
	private static class PropertyChangeEnsuringBeanWrapper extends DirectFieldAccessFallbackBeanWrapper {

		public PropertyChangeEnsuringBeanWrapper(Object entity) {
			super(entity);
		}

		/**
		 * We in case of setting the value using field access, we need to make sure that EclipseLink detects the change.
		 * Hence we check for an EclipseLink specific generated method that is used to record the changes and invoke it if
		 * available.
		 * 
		 * @see org.springframework.data.support.DirectFieldAccessFallbackBeanWrapper#setPropertyUsingFieldAccess(java.lang.String,
		 *      java.lang.Object)
		 */
		@Override
		public void setPropertyValue(String propertyName, Object value) {

			Object oldValue = getPropertyValue(propertyName);
			super.setPropertyValue(propertyName, value);
			triggerPropertyChangeMethodIfAvailable(propertyName, oldValue, value);
		}

		private void triggerPropertyChangeMethodIfAvailable(String propertyName, Object oldValue, Object value) {

			Method method = ReflectionUtils.findMethod(getWrappedClass(), "_persistence_propertyChange", String.class,
					Object.class, Object.class);

			if (method == null) {
				return;
			}

			ReflectionUtils.invokeMethod(method, getWrappedInstance(), propertyName, oldValue, value);
		}
	}
}
