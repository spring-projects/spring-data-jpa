/*
 * Copyright 2017-2024 the original author or authors.
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

import jakarta.persistence.criteria.CriteriaBuilder;

import java.util.Collections;

import org.eclipse.persistence.internal.jpa.querydef.ParameterExpressionImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.repository.query.Parameters;
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

		CriteriaBuilder builder = mock(CriteriaBuilder.class);

		Parameters<?, ?> parameters = mock(Parameters.class, RETURNS_DEEP_STUBS);
		when(parameters.getBindableParameters().iterator()).thenReturn(Collections.emptyListIterator());

		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(builder, parameters,
				EscapeCharacter.DEFAULT);

		assertThatExceptionOfType(RuntimeException.class) //
				.isThrownBy(() -> metadataProvider.next(mock(Part.class))) //
				.withMessageContaining("parameter");
	}

	@Test // GH-3137
	void returnAugmentedValueForStringExpressions() {

		when(part.getProperty().getLeafProperty().isCollection()).thenReturn(false);

		assertThat(createParameterMetadata(Part.Type.STARTING_WITH).prepare("starting with")).isEqualTo("starting with%");
		assertThat(createParameterMetadata(Part.Type.ENDING_WITH).prepare("ending with")).isEqualTo("%ending with");
		assertThat(createParameterMetadata(Part.Type.CONTAINING).prepare("containing")).isEqualTo("%containing%");
		assertThat(createParameterMetadata(Part.Type.NOT_CONTAINING).prepare("not containing"))
				.isEqualTo("%not containing%");
		assertThat(createParameterMetadata(Part.Type.LIKE).prepare("%like%")).isEqualTo("%like%");
		assertThat(createParameterMetadata(Part.Type.IS_NULL).prepare(null)).isEqualTo(null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ParameterMetadataProvider.ParameterMetadata createParameterMetadata(Part.Type partType) {

		when(part.getType()).thenReturn(partType);
		return new ParameterMetadataProvider.ParameterMetadata<>(parameterExpression, part, null, EscapeCharacter.DEFAULT);
	}
}
