/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.*;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.data.jpa.domain.support.AuditingBeanFactoryPostProcessor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.util.HidingClassLoader;

/**
 * Unit tests for {@link JpaRuntimeHints}.
 *
 * @author Christoph Strobl
 */
class JpaRuntimeHintsUnitTests {

	@Test // GH-2497
	void registersAuditing() {

		RuntimeHints hints = new RuntimeHints();

		JpaRuntimeHints registrar = new JpaRuntimeHints();
		registrar.registerHints(hints, null);

		assertThat(hints).matches(reflection().onType(AnnotationBeanConfigurerAspect.class))
				.matches(reflection().onType(AuditingEntityListener.class))
				.matches(reflection().onType(AuditingBeanFactoryPostProcessor.class));
	}

	@Test // GH-2497
	void skipsAuditingHintsIfAspectjNotPresent() {

		RuntimeHints hints = new RuntimeHints();

		JpaRuntimeHints registrar = new JpaRuntimeHints();
		registrar.registerHints(hints, HidingClassLoader.hidePackages("org.springframework.beans.factory.aspectj"));

		assertThat(hints).matches(reflection().onType(AnnotationBeanConfigurerAspect.class).negate())
				.matches(reflection().onType(AuditingEntityListener.class).negate())
				.matches(reflection().onType(AuditingBeanFactoryPostProcessor.class).negate());
	}
}
