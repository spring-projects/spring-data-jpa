/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.query.ParameterBinding;

/**
 * AOT query value object along with its parameter bindings.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 4.0
 */
abstract class AotQuery {

	private final List<ParameterBinding> parameterBindings;

	AotQuery(List<ParameterBinding> parameterBindings) {
		this.parameterBindings = parameterBindings;
	}

	/**
	 * @return whether the query is a {@link jakarta.persistence.EntityManager#createNativeQuery native} one.
	 */
	public abstract boolean isNative();

	public List<ParameterBinding> getParameterBindings() {
		return parameterBindings;
	}

	/**
	 * @return the preliminary query limit.
	 */
	public Limit getLimit() {
		return Limit.unlimited();
	}

	/**
	 * @return whether the query is limited (e.g. {@code findTop10By}).
	 */
	public boolean isLimited() {
		return getLimit().isLimited();
	}

	/**
	 * @return whether the query a delete query.
	 */
	public boolean isDelete() {
		return false;
	}

	/**
	 * @return whether the query is an exists query.
	 */
	public boolean isExists() {
		return false;
	}

	/**
	 * @return {@literal true} if the query uses value expressions.
	 */
	public boolean hasExpression() {

		for (ParameterBinding parameterBinding : parameterBindings) {
			if (parameterBinding.getOrigin().isExpression()) {
				return true;
			}
		}

		return false;
	}

}
