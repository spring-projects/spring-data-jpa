/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.data.jpa.mapping;

import static org.mockito.Mockito.*;

import java.util.Collections;

import javax.persistence.metamodel.Metamodel;

import org.junit.Test;
import org.springframework.data.annotation.Version;

/**
 * Unit tests for {@link JpaMetamodelMappingContext}.
 *
 * @author Oliver Gierke
 */
public class JpaMetamodelMappingContextUnitTests {

	@Test // DATAJPA-775
	public void jpaPersistentEntityRejectsSprignDataAtVersionAnnotation() {

		Metamodel metamodel = mock(Metamodel.class);

		JpaMetamodelMappingContext context = new JpaMetamodelMappingContext(Collections.singleton(metamodel));
		context.getPersistentEntity(Sample.class);
	}

	static class Sample {
		@Version Long version;
	}
}
