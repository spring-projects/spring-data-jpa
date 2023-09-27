/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.provider;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.springframework.core.annotation.MergedAnnotations;

/**
 * A JUnit 5 extension to handle hiding certain {@link PersistenceProvider}s.
 * 
 * @author Greg Turnquist
 */
class PersistenceProviderHiderExtension implements InvocationInterceptor {

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {

		Method testMethod = invocationContext.getExecutable();
		Class<?> testClass = extensionContext.getRequiredTestClass();

		Set<AnnotatedElement> candidates = Set.of(testMethod, testClass);

		Optional<List<PersistenceProvider>> persistenceProviders = candidates.stream() //
				.filter(annotatedElement -> {
					MergedAnnotations annotations = MergedAnnotations.from(annotatedElement,
							MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);

					return annotations.isPresent(HidePersistenceProviders.class) || annotations.isPresent(HideHibernate.class)
							|| annotations.isPresent(HideEclipseLink.class);
				}) //
				.findFirst() //
				.map(source -> {

					MergedAnnotations annotations = MergedAnnotations.from(source,
							MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);

					if (annotations.get(HideHibernate.class).isPresent()) {
						return List.of(PersistenceProvider.HIBERNATE);
					}

					if (annotations.get(HideEclipseLink.class).isPresent()) {
						return List.of(PersistenceProvider.ECLIPSELINK);
					}

					if (annotations.isPresent(HidePersistenceProviders.class)) {
						return List.of( //
								annotations.get(HidePersistenceProviders.class).getEnumArray("value", PersistenceProvider.class));
					}

					return List.of();
				});

		Runnable testMethodInvocation = () -> {
			try {
				invocation.proceed();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};

		persistenceProviders.ifPresentOrElse(
				providers -> PersistenceProviderUtils.doWithPersistenceProvidersHidden(providers, testMethodInvocation),
				testMethodInvocation);
	}

}
