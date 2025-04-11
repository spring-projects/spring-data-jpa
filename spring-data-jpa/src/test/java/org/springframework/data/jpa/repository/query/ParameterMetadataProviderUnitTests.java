/*
 * Copyright 2017-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.eclipse.persistence.internal.jpa.querydef.ParameterExpressionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.parser.Part;

/**
 * Unit tests for {@link ParameterMetadataProvider}.
 *
 * @author Jens Schauder
 * @author Julia Lee
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ParameterMetadataProviderUnitTests {

	@Mock(answer = Answers.RETURNS_DEEP_STUBS) Part part;

	@SuppressWarnings("rawtypes") //
	private final ParameterExpressionImpl parameterExpression = new ParameterExpressionImpl<>(null, String.class);

	@Test // DATAJPA-863
	void errorMessageMentionsParametersWhenParametersAreExhausted() {

		JpaParameters parameters = mock(JpaParameters.class, RETURNS_DEEP_STUBS);
		when(parameters.getBindableParameters().iterator()).thenReturn(Collections.emptyListIterator());

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(parameters,
				EscapeCharacter.DEFAULT, JpqlQueryTemplates.UPPER);

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> metadataProvider.next(mock(Part.class))) //
				.withMessageContaining("parameter");
	}

}
