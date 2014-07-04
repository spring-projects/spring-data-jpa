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
package org.springframework.data.jpa.repository.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.repository.query.Parameter;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.TypeUtils;

/**
 * A {@link EvaluationContextProvider} that assembles an {@link EvaluationContext} from a set of
 * {@link EvaluationContextExtension} beans that are defined in the application context.
 * 
 * @author Thomas Darimont
 * @since 1.7
 */
public class ExtensibleEvaluationContextProvider implements EvaluationContextProvider, ApplicationContextAware {

	private ListableBeanFactory beanFactory;

	/**
	 * Creates a new {@link ExtensibleEvaluationContextProvider}.
	 */
	public ExtensibleEvaluationContextProvider() {}

	/* (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.support.EvaluationContextProvider#getEvaluationContext()
	 */
	@Override
	public StandardEvaluationContext getEvaluationContext(Object[] parameterValues,
			Iterable<? extends Parameter> parameters) {

		StandardEvaluationContext ec = new StandardEvaluationContext();
		ec.setBeanResolver(new BeanFactoryResolver(beanFactory));

		List<EvaluationContextExtension> extensions = new ArrayList<EvaluationContextExtension>(beanFactory.getBeansOfType(
				EvaluationContextExtension.class).values());

		if (extensions.isEmpty()) {
			return ec;
		}

		ec.setPropertyAccessors(new ArrayList<PropertyAccessor>());
		ec.setMethodResolvers(new ArrayList<MethodResolver>());

		new EvaluationContextExtensionMerger(extensions).copyInto(ec);

		ec.getPropertyAccessors().add(new ReflectivePropertyAccessor());

		ec.setRootObject(parameterValues);

		for (Parameter param : parameters) {
			if (param.isNamedParameter()) {
				ec.setVariable(param.getName(), parameterValues[param.getIndex()]);
			}
		}

		return ec;
	}

	/**
	 * @author Thomas Darimont
	 */
	static class EvaluationContextExtensionMerger extends ReadOnlyPropertyAccessor implements MethodResolver {

		private final Map<String, Object> properties;
		private final Map<String, Object> variables;
		private final Map<String, Object> functions;

		public EvaluationContextExtensionMerger(List<EvaluationContextExtension> extensions) {

			Map<String, Object> properties = new HashMap<String, Object>();
			Map<String, Object> variables = new HashMap<String, Object>();
			Map<String, Object> functions = new HashMap<String, Object>();

			for (EvaluationContextExtension ext : extensions) {

				if (ext.getScope() != null) {
					properties.put(ext.getScope(), ext.getProperties());
					variables.put(ext.getScope(), ext.getVariables());
					functions.put(ext.getScope(), ext.getFunctions());
				}

				properties.putAll(ext.getProperties());
				variables.putAll(ext.getVariables());
				functions.putAll(ext.getFunctions());
			}

			this.properties = properties;
			this.variables = variables;
			this.functions = functions;
		}

		/**
		 * Copies the collected information form the {@link EvaluationContextExtension}s into the given
		 * {@link StandardEvaluationContext}.
		 * 
		 * @param ec
		 */
		public void copyInto(StandardEvaluationContext ec) {

			ec.getPropertyAccessors().add(this);
			ec.getMethodResolvers().add(this);
			ec.setVariables(variables);
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {

			if (target instanceof Map) {
				return ((Map) target).containsKey(name);
			}

			return properties.containsKey(name);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {

			Object value = null;

			if (target instanceof Map) {
				value = ((Map) target).get(name);
			}

			if (value == null) {
				value = properties.get(name);
			}

			return new TypedValue(value);
		}

		@Override
		public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name,
				List<TypeDescriptor> argumentTypes) throws AccessException {

			final Method function = targetObject instanceof Map && ((Map) targetObject).containsKey(name) ? (Method) ((Map) targetObject)
					.get(name) : (Method) functions.get(name);

			Class<?>[] parameterTypes = function.getParameterTypes();
			if (parameterTypes.length != argumentTypes.size()) {
				return null;
			}

			for (int i = 0; i < parameterTypes.length; i++) {
				if (!TypeUtils.isAssignable(parameterTypes[i], argumentTypes.get(i).getType())) {
					return null;
				}
			}

			return new MethodExecutor() {

				@Override
				public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
					try {
						return new TypedValue(function.invoke(null, arguments));
					} catch (Exception e) {
						throw new SpelEvaluationException(e, SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, function.getName(),
								function.getDeclaringClass());
					}
				}
			};
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	static abstract class ReadOnlyPropertyAccessor implements PropertyAccessor {

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			return null;
		}

		@Override
		public abstract boolean canRead(EvaluationContext context, Object target, String name) throws AccessException;

		@Override
		public abstract TypedValue read(EvaluationContext context, Object target, String name) throws AccessException;

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
			// noop
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.beanFactory = applicationContext;
	}
}
