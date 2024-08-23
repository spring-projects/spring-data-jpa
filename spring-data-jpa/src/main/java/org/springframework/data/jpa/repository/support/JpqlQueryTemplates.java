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
package org.springframework.data.jpa.repository.support;

import java.util.function.Function;

/**
 * @author Mark Paluch
 */
public class JpqlQueryTemplates {

	public static final JpqlQueryTemplates UPPER = new JpqlQueryTemplates("UPPER", String::toUpperCase);

	public static final JpqlQueryTemplates LOWER = new JpqlQueryTemplates("LOWER", String::toLowerCase);

	private final String ignoreCaseOperator;

	private final Function<String, String> ignoreCaseFunction;

	JpqlQueryTemplates(String ignoreCaseOperator, Function<String, String> ignoreCaseFunction) {
		this.ignoreCaseOperator = ignoreCaseOperator;
		this.ignoreCaseFunction = ignoreCaseFunction;
	}

	public static JpqlQueryTemplates of(String ignoreCaseOperator, Function<String, String> ignoreCaseFunction) {
		return new JpqlQueryTemplates(ignoreCaseOperator, ignoreCaseFunction);
	}

	public String ignoreCase(String value) {
		return ignoreCaseFunction.apply(value);
	}

	public String getIgnoreCaseOperator() {
		return ignoreCaseOperator;
	}
}
