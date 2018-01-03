/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.config;

/**
 * Helper class to manage bean definition names in a single place.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Andrew Walters
 */
interface BeanDefinitionNames {

	public static final String JPA_MAPPING_CONTEXT_BEAN_NAME = "jpaMappingContext";
	public static final String JPA_CONTEXT_BEAN_NAME = "jpaContext";
	public static final String EM_BEAN_DEFINITION_REGISTRAR_POST_PROCESSOR_BEAN_NAME = "emBeanDefinitionRegistrarPostProcessor";
}
