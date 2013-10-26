package org.springframework.data.jpa.repository.augment;

import java.lang.reflect.Method;

import org.springframework.data.support.DirectFieldAccessFallbackBeanWrapper;
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
	protected void setPropertyUsingFieldAccess(String propertyName, Object value) {

		Object oldValue = getPropertyValue(propertyName);
		super.setPropertyUsingFieldAccess(propertyName, oldValue);
		triggerPropertyChangeMethodIfAvailable(propertyName, oldValue, oldValue);
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