/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.jpa.support;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link PersistenceUnitPostProcessor} that will scan for classes annotated with {@link Entity} or
 * {@link MappedSuperclass} and add them to the {@link javax.persistence.PersistenceUnit} post prcessed. Beyond that JPA
 * XML mapping files can be scanned as well by configuring a file name pattern.
 * 
 * @author Oliver Gierke
 */
public class ClasspathScanningPersistenceUnitPostProcessor implements PersistenceUnitPostProcessor, ResourceLoaderAware {

	private static final Logger LOG = LoggerFactory.getLogger(ClasspathScanningPersistenceUnitPostProcessor.class);

	private final String basePackage;
	private ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
	private String mappingFileNamePattern;

	/**
	 * Creates a new {@link ClasspathScanningPersistenceUnitPostProcessor} using the given base package as scan base.
	 * 
	 * @param basePackage must not be {@literal null} or empty.
	 */
	public ClasspathScanningPersistenceUnitPostProcessor(String basePackage) {
		Assert.hasText(basePackage);
		this.basePackage = basePackage;
	}

	/**
	 * Configures the file name pattern JPA entity mapping files shall scanned from the classpath. Lookup will use the
	 * configured base package as root.
	 * 
	 * @param mappingFilePattern must not be {@literal null} or empty.
	 */
	public void setMappingFileNamePattern(String mappingFilePattern) {
		Assert.hasText(mappingFilePattern);
		this.mappingFileNamePattern = mappingFilePattern;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader);
		this.resolver = new PathMatchingResourcePatternResolver(resourceLoader);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor#postProcessPersistenceUnitInfo(org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo)
	 */
	public void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {

		ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
		provider.addIncludeFilter(new AnnotationTypeFilter(MappedSuperclass.class));

		for (BeanDefinition definition : provider.findCandidateComponents(basePackage)) {

			LOG.debug("Registering classpath-scanned entity %s in persistence unit info!", definition.getBeanClassName());
			pui.addManagedClassName(definition.getBeanClassName());
		}

		for (String location : scanForMappingFileLocations()) {
			LOG.debug("Registering classpath-scanned entity mapping file in persistence unit info!", location);
			pui.addMappingFileName(location);
		}

	}

	/**
	 * Scanes the configured base package for files matching the configured mapping file name pattern. Will simply return
	 * an empty {@link Set} in case no {@link ResourceLoader} or mapping file name pattern was configured.
	 * 
	 * @return
	 */
	private Set<String> scanForMappingFileLocations() {

		if (resolver == null || !StringUtils.hasText(mappingFileNamePattern)) {
			return Collections.emptySet();
		}

		String path = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + basePackage.replace('.', File.separatorChar)
				+ File.separator + mappingFileNamePattern;
		Set<String> mappingFileUris = new HashSet<String>();
		Resource[] scannedResources = new Resource[0];

		try {
			scannedResources = resolver.getResources(path);
		} catch (IOException e) {
			throw new IllegalStateException(String.format("Cannot load mapping files from path %s!", path), e);
		}

		for (Resource resource : scannedResources) {
			try {
				mappingFileUris.add(resource.getURI().toString());
			} catch (IOException e) {
				throw new IllegalStateException(String.format("Couldn't get URI for %s!", resource.toString()), e);
			}
		}

		return mappingFileUris;
	}
}
