/*
 * Copyright 2008-2011 the original author or authors.
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

import org.springframework.data.repository.config.AutomaticRepositoryConfigInformation;
import org.springframework.data.repository.config.ManualRepositoryConfigInformation;
import org.springframework.data.repository.config.RepositoryConfig;
import org.springframework.data.repository.config.SingleRepositoryConfigInformation;
import org.w3c.dom.Element;

/**
 * @author Oliver Gierke
 */
public class SimpleJpaRepositoryConfiguration extends
		RepositoryConfig<SimpleJpaRepositoryConfiguration.JpaRepositoryConfiguration, SimpleJpaRepositoryConfiguration> {

	private static final String FACTORY_CLASS = "org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean";
	private static final String ENTITY_MANAGER_FACTORY_REF = "entity-manager-factory-ref";

	/**
	 * @param repositoriesElement
	 */
	public SimpleJpaRepositoryConfiguration(Element repositoriesElement) {

		super(repositoriesElement, FACTORY_CLASS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.config.GlobalRepositoryConfigInformation
	 * #getAutoconfigRepositoryInformation(java.lang.String)
	 */
	public JpaRepositoryConfiguration getAutoconfigRepositoryInformation(String interfaceName) {

		return new AutomaticJpaRepositoryConfigInformation(interfaceName, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.jpa.repository.config.RepositoryConfigContext
	 * #getManualRepositoryInformation(org.w3c.dom.Element,
	 * org.springframework.data
	 * .jpa.repository.config.CommonRepositoryInformation)
	 */
	@Override
	public JpaRepositoryConfiguration createSingleRepositoryConfigInformationFor(Element element) {

		return new ManualJpaRepositoryConfigInformation(element, this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.config.CommonRepositoryConfigInformation
	 * #getNamedQueriesLocation()
	 */
	public String getNamedQueriesLocation() {

		return "classpath*:META-INF/jpa-named-queries.properties";
	}

	/**
	 * Returns the name of the entity manager factory bean.
	 * 
	 * @return
	 */
	public String getEntityManagerFactoryRef() {

		return getSource().getAttribute(ENTITY_MANAGER_FACTORY_REF);
	}

	private static class AutomaticJpaRepositoryConfigInformation extends
			AutomaticRepositoryConfigInformation<SimpleJpaRepositoryConfiguration> implements JpaRepositoryConfiguration {

		public AutomaticJpaRepositoryConfigInformation(String interfaceName, SimpleJpaRepositoryConfiguration parent) {

			super(interfaceName, parent);
		}

		/**
		 * Returns the {@link javax.persistence.EntityManagerFactory} reference to be used for all the repository instances
		 * configured.
		 * 
		 * @return
		 */
		public String getEntityManagerFactoryRef() {

			return getParent().getEntityManagerFactoryRef();
		}
	}

	private static class ManualJpaRepositoryConfigInformation extends
			ManualRepositoryConfigInformation<SimpleJpaRepositoryConfiguration> implements JpaRepositoryConfiguration {

		public ManualJpaRepositoryConfigInformation(Element element, SimpleJpaRepositoryConfiguration parent) {

			super(element, parent);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.data.jpa.repository.config.
		 * SimpleJpaRepositoryConfiguration
		 * .JpaRepositoryConfiguration#getEntityManagerFactoryRef()
		 */
		public String getEntityManagerFactoryRef() {

			return getAttribute(ENTITY_MANAGER_FACTORY_REF);
		}
	}

	static interface JpaRepositoryConfiguration extends
			SingleRepositoryConfigInformation<SimpleJpaRepositoryConfiguration> {

		String getEntityManagerFactoryRef();
	}
}
