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
package org.springframework.data.jpa.repository.cdi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.persistence.EntityManager;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.repository.cdi.CdiRepositoryBean;
import org.springframework.data.repository.cdi.CdiRepositoryExtensionSupport;

/**
 * A portable CDI extension which registers beans for Spring Data JPA repositories.
 *
 * @author Dirk Mahler
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Donghun Shin
 */
public class JpaRepositoryExtension extends CdiRepositoryExtensionSupport {

	private static final Log LOGGER = LogFactory.getLog(JpaRepositoryExtension.class);

	private final Map<Set<Annotation>, Bean<EntityManager>> entityManagers = new HashMap<>();

	public JpaRepositoryExtension() {
		LOGGER.info("Activating CDI extension for Spring Data JPA repositories");
	}

	/**
	 * Implementation of an observer which checks for EntityManager beans and stores them in {@link #entityManagers} for
	 * later association with corresponding repository beans.
	 *
	 * @param <X> The type.
	 * @param processBean The annotated type as defined by CDI.
	 */
	@SuppressWarnings("unchecked")
	<X> void processBean(@Observes ProcessBean<X> processBean) {

		Bean<X> bean = processBean.getBean();
		for (Type type : bean.getTypes()) {
			// Check if the bean is an EntityManager.
			if (type instanceof Class<?> classType && EntityManager.class.isAssignableFrom(classType)) {
				Set<Annotation> qualifiers = new HashSet<>(bean.getQualifiers());
				if (bean.isAlternative() || !entityManagers.containsKey(qualifiers)) {
					LOGGER.debug(String.format("Discovered '%s' with qualifiers %s", EntityManager.class.getName(), qualifiers));
					entityManagers.put(qualifiers, (Bean<EntityManager>) bean);
				}
			}
		}
	}

	/**
	 * Implementation of an observer which registers beans to the CDI container for the detected Spring Data repositories.
	 * <p>
	 * The repository beans are associated to the EntityManagers using their qualifiers.
	 *
	 * @param beanManager The BeanManager instance.
	 */
	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {

		for (Entry<Class<?>, Set<Annotation>> entry : getRepositoryTypes()) {

			Class<?> repositoryType = entry.getKey();
			Set<Annotation> qualifiers = entry.getValue();

			// Create the bean representing the repository.
			CdiRepositoryBean<?> repositoryBean = createRepositoryBean(repositoryType, qualifiers, beanManager);
			LOGGER.info(String.format("Registering bean for '%s' with qualifiers %s", repositoryType.getName(), qualifiers));

			// Register the bean to the extension and the container.
			registerBean(repositoryBean);
			afterBeanDiscovery.addBean(repositoryBean);
		}
	}

	/**
	 * Creates a {@link Bean}.
	 *
	 * @param <T> The type of the repository.
	 * @param repositoryType The class representing the repository.
	 * @param beanManager The BeanManager instance.
	 * @return The bean.
	 */
	private <T> CdiRepositoryBean<T> createRepositoryBean(Class<T> repositoryType, Set<Annotation> qualifiers,
			BeanManager beanManager) {

		// Determine the entity manager bean which matches the qualifiers of the repository.
		Bean<EntityManager> entityManagerBean = entityManagers.get(qualifiers);

		if (entityManagerBean == null) {
			throw new UnsatisfiedResolutionException(String.format("Unable to resolve a bean for '%s' with qualifiers %s",
					EntityManager.class.getName(), qualifiers));
		}

		// Construct and return the repository bean.
		return new JpaRepositoryBean<>(beanManager, entityManagerBean, qualifiers, repositoryType,
				Optional.of(getCustomImplementationDetector()));
	}
}
