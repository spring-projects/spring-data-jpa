/*
 * Copyright 2013-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.jpa.repository.query.DefaultJpaEntityMetadata;

/**
 * Unit tests for {@link DefaultJpaEntityMetadata}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 */
class DefaultJpaEntityMetadataUnitTest {

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void rejectsNullDomainType() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DefaultJpaEntityMetadata(null));
	}

	@Test
	void returnsConfiguredType() {

		DefaultJpaEntityMetadata<Foo> metadata = new DefaultJpaEntityMetadata<>(Foo.class);
		assertThat(metadata.getJavaType()).isEqualTo(Foo.class);
	}

	@Test
	void returnsSimpleClassNameAsEntityNameByDefault() {

		DefaultJpaEntityMetadata<Foo> metadata = new DefaultJpaEntityMetadata<>(Foo.class);
		assertThat(metadata.getEntityName()).isEqualTo(Foo.class.getSimpleName());
	}

	@Test
	void returnsCustomizedEntityNameIfConfigured() {

		DefaultJpaEntityMetadata<Bar> metadata = new DefaultJpaEntityMetadata<>(Bar.class);
		assertThat(metadata.getEntityName()).isEqualTo("Entity");
	}

	@Test // DATAJPA-871
	void returnsCustomizedEntityNameIfConfiguredViaComposedAnnotation() {

		DefaultJpaEntityMetadata<BarWithComposedAnnotation> metadata = new DefaultJpaEntityMetadata<>(
				BarWithComposedAnnotation.class);
		assertThat(metadata.getEntityName()).isEqualTo("Entity");
	}

	@Entity
	@Retention(RetentionPolicy.RUNTIME)
	static @interface CustomEntityAnnotationUsingAliasFor {

		@AliasFor(annotation = Entity.class, attribute = "name")
		String entityName();
	}

	private static class Foo {
	}

	@Entity(name = "Entity")
	static class Bar {
	}

	@CustomEntityAnnotationUsingAliasFor(entityName = "Entity")
	private static class BarWithComposedAnnotation {
	}
}
