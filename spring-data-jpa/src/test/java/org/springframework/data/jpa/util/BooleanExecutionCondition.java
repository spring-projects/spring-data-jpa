/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.util;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.*;
import static org.junit.platform.commons.util.AnnotationUtils.*;

import java.lang.annotation.Annotation;
import java.util.function.Function;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

abstract class BooleanExecutionCondition<A extends Annotation> implements ExecutionCondition {

	private final Class<A> annotationType;
	private final String enabledReason;
	private final String disabledReason;
	private final Function<A, String> customDisabledReason;

	BooleanExecutionCondition(Class<A> annotationType, String enabledReason, String disabledReason,
			Function<A, String> customDisabledReason) {
		this.annotationType = annotationType;
		this.enabledReason = enabledReason;
		this.disabledReason = disabledReason;
		this.customDisabledReason = customDisabledReason;
	}

	abstract boolean isEnabled(A annotation);

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		return findAnnotation(context.getElement(), annotationType) //
				.map(annotation -> isEnabled(annotation) ? enabled(enabledReason)
						: disabled(disabledReason, customDisabledReason.apply(annotation))) //
				.orElseGet(this::enabledByDefault);
	}

	private ConditionEvaluationResult enabledByDefault() {
		String reason = String.format("@%s is not present", annotationType.getSimpleName());
		return enabled(reason);
	}

}
