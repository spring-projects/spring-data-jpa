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

import java.lang.reflect.Method;

import org.springframework.data.util.DirectFieldAccessFallbackBeanWrapper;
import org.springframework.util.ReflectionUtils;

/**
 * Custom {@link DirectFieldAccessFallbackBeanWrapper} to hook in additional functionality when setting a property by
 * field access.
 * 
 * @author Oliver Gierke
 */
class PropertyChangeEnsuringBeanWrapper extends DirectFieldAccessFallbackBeanWrapper {

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
