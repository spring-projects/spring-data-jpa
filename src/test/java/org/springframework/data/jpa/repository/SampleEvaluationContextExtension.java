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
package org.springframework.data.jpa.repository;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.jpa.repository.SampleSecurity.SampleSecurityContextHolder;
import org.springframework.data.jpa.repository.support.DefaultEvaluationContextExtension;
import org.springframework.data.jpa.repository.support.EvaluationContextExtension;

/**
 * A sample implementation of a custom {@link EvaluationContextExtension}.
 * 
 * @author Thomas Darimont
 */
public class SampleEvaluationContextExtension extends DefaultEvaluationContextExtension {

	@Override
	public String getScope() {
		return "security";
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.singletonMap("principal", SampleSecurityContextHolder.getCurrent().getPrincipal());
	}
}
