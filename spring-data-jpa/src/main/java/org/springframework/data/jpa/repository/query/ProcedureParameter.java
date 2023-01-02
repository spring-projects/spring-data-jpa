/*
 * Copyright 2021-2023 the original author or authors.
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

import java.util.Objects;

import jakarta.persistence.ParameterMode;

import org.springframework.lang.Nullable;

/**
 * This class represents a Stored Procedure Parameter and an instance of the annotation
 * {@link jakarta.persistence.StoredProcedureParameter}.
 *
 * @author Gabriel Basilio
 * @author Greg Turnquist
 */
class ProcedureParameter {

	private final String name;
	private final ParameterMode mode;
	private final Class<?> type;

	ProcedureParameter(@Nullable String name, ParameterMode mode, Class<?> type) {

		this.name = name;
		this.mode = mode;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public ParameterMode getMode() {
		return mode;
	}

	public Class<?> getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof ProcedureParameter)) {
			return false;
		}

		ProcedureParameter that = (ProcedureParameter) o;
		return Objects.equals(name, that.name) && mode == that.mode && Objects.equals(type, that.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, mode, type);
	}

	@Override
	public String toString() {
		return "ProcedureParameter{" + "name='" + name + '\'' + ", mode=" + mode + ", type=" + type + '}';
	}
}
