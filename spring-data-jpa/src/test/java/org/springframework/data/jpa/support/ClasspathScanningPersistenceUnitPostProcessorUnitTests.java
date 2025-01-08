/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.jpa.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link ClasspathScanningPersistenceUnitPostProcessor}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClasspathScanningPersistenceUnitPostProcessorUnitTests {

	@Mock MutablePersistenceUnitInfo pui;
	private String basePackage = getClass().getPackage().getName();

	@Test
	void rejectsNullBasePackage() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClasspathScanningPersistenceUnitPostProcessor(null));
	}

	@Test
	void rejectsEmptyBasePackage() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ClasspathScanningPersistenceUnitPostProcessor(""));
	}

	@Test
	void rejectsNullMappingFileNamePattern() {
		ClasspathScanningPersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(
				basePackage);
		assertThatIllegalArgumentException().isThrownBy(() -> processor.setMappingFileNamePattern(null));
	}

	@Test
	void rejectsEmptyMappingFileNamePattern() {
		ClasspathScanningPersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(
				basePackage);
		assertThatIllegalArgumentException().isThrownBy(() -> processor.setMappingFileNamePattern(""));
	}

	@Test
	void findsEntityClassesForBasePackage() {

		PersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(basePackage);
		processor.postProcessPersistenceUnitInfo(pui);

		verify(pui).addManagedClassName(SampleEntity.class.getName());
	}

	@Test // DATAJPA-407
	void findsMappingFile() {

		ClasspathScanningPersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(
				basePackage);
		processor.setMappingFileNamePattern("*.xml");
		processor.setResourceLoader(new DefaultResourceLoader());
		processor.postProcessPersistenceUnitInfo(pui);

		String expected = getClass().getPackage().getName().replace('.', '/') + "/mapping.xml";

		verify(pui).addManagedClassName(SampleEntity.class.getName());
		verify(pui).addMappingFileName(expected);
	}

	@Test // DATAJPA-353, DATAJPA-407
	void shouldFindJpaMappingFilesFromMultipleLocationsOnClasspath() {

		ClasspathScanningPersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(
				basePackage);

		processor.setResourceLoader(new DefaultResourceLoader());
		processor.setMappingFileNamePattern("**/*orm.xml");
		processor.postProcessPersistenceUnitInfo(pui);

		verify(pui).addMappingFileName("org/springframework/data/jpa/support/module1/module1-orm.xml");
		verify(pui).addMappingFileName("org/springframework/data/jpa/support/module2/module2-orm.xml");
	}

	@Test // DATAJPA-519
	void shouldFindJpaMappingFilesFromNestedJarLocationsOnClasspath() {

		String nestedModule3Path = "org/springframework/data/jpa/support/module3/module3-orm.xml";
		final String fileInJarUrl = "jar:file:/foo/bar/lib/somelib.jar!/" + nestedModule3Path;

		ResourceLoader resolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader()) {

			@Override
			public Resource[] getResources(String locationPattern) throws IOException {

				Resource[] resources = super.getResources(locationPattern);
				resources = Arrays.copyOf(resources, resources.length + 1);
				resources[resources.length - 1] = new UrlResource(fileInJarUrl);

				return resources;
			}

			@Override
			protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootUri, String subPattern)
					throws IOException {

				if (fileInJarUrl.equals(rootUri.toString())) {
					return Collections.singleton(rootDirResource);
				}

				return super.doFindPathMatchingJarResources(rootDirResource, rootUri, subPattern);
			}
		};

		ClasspathScanningPersistenceUnitPostProcessor processor = new ClasspathScanningPersistenceUnitPostProcessor(
				basePackage);
		ReflectionTestUtils.setField(processor, "mappingFileResolver", resolver);
		processor.setMappingFileNamePattern("**/*orm.xml");
		processor.postProcessPersistenceUnitInfo(pui);

		verify(pui).addMappingFileName("org/springframework/data/jpa/support/module1/module1-orm.xml");
		verify(pui).addMappingFileName("org/springframework/data/jpa/support/module2/module2-orm.xml");
		verify(pui).addMappingFileName(nestedModule3Path);
	}

	@Entity
	public static class SampleEntity {}
}
