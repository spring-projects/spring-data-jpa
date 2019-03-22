/*
 * Copyright 2014-2019 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import java.util.Collections;
import java.util.Map;

import org.springframework.data.spel.spi.EvaluationContextExtension;

/**
 * A sample implementation of a custom {@link EvaluationContextExtension}.
 *
 * @author Thomas Darimont
 */
public class SampleEvaluationContextExtension implements EvaluationContextExtension {

	@Override
	public String getExtensionId() {
		return "security";
	}

	@Override
	public Map<String, Object> getProperties() {
		return Collections.singletonMap("principal", SampleSecurityContextHolder.getCurrent().getPrincipal());
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class SampleSecurityContextHolder {

		private static ThreadLocal<SampleAuthentication> auth = new ThreadLocal<SampleAuthentication>() {

			protected SampleAuthentication initialValue() {
				return new SampleAuthentication(new SampleUser(-1, "anonymous"));
			}

		};

		public static SampleAuthentication getCurrent() {
			return auth.get();
		}

		public static void clear() {
			auth.remove();
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class SampleAuthentication {

		private Object principal;

		public SampleAuthentication(Object principal) {
			this.principal = principal;
		}

		public Object getPrincipal() {
			return principal;
		}

		public void setPrincipal(Object principal) {
			this.principal = principal;
		}
	}

	/**
	 * @author Thomas Darimont
	 */
	public static class SampleUser {

		private Object id;
		private String name;

		public SampleUser(Object id, String name) {
			this.id = id;
			this.name = name;
		}

		public Object getId() {
			return id;
		}

		public void setId(Object id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public SampleUser withName(String name) {

			this.name = name;
			return this;
		}

		public SampleUser withId(Object id) {

			this.id = id;
			return this;
		}
	}
}
