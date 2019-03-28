/*
 * Copyright 2019 the original author or authors.
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

import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.spel.spi.EvaluationContextExtension;

/**
 * {@link EvaluationContextExtension} to register {@link EscapeCharacter} as root object to essentially expose an
 * {@code expose(…)} function to SpEL.
 *
 * @author Oliver Drotbohm
 */
public class JpaEvaluationContextExtension implements EvaluationContextExtension {

	private final EscapeCharacter character;

	/**
	 * Creates a new {@link JpaEvaluationContextExtension} for the given escape character.
	 *
	 * @param escapeCharacter the character to be used to escape parameters for LIKE expression.
	 */
	public JpaEvaluationContextExtension(char escapeCharacter) {
		this.character = EscapeCharacter.of(escapeCharacter);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.spel.spi.EvaluationContextExtension#getExtensionId()
	 */
	@Override
	public String getExtensionId() {
		return "jpa";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.spel.spi.EvaluationContextExtension#getRootObject()
	 */
	@Override
	public Object getRootObject() {
		return character;
	}
}
