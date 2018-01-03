/*
 * Copyright 2011-2018 the original author or authors.
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

import java.net.URISyntaxException;
import java.net.URL;

import javax.persistence.spi.PersistenceUnitInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

/**
 * Extends {@link DefaultPersistenceUnitManager} to merge configurations of one persistence unit residing in multiple
 * {@code persistence.xml} files into one. This is necessary to allow the declaration of entities in seperate modules.
 *
 * @author Oliver Gierke
 * @link http://jira.springframework.org/browse/SPR-2598
 */
public class MergingPersistenceUnitManager extends DefaultPersistenceUnitManager {

	private static final Logger LOG = LoggerFactory.getLogger(MergingPersistenceUnitManager.class);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager#postProcessPersistenceUnitInfo(org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo)
	 */
	@Override
	protected void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui) {

		// Invoke normal post processing
		super.postProcessPersistenceUnitInfo(pui);

		PersistenceUnitInfo oldPui = getPersistenceUnitInfo(((PersistenceUnitInfo) pui).getPersistenceUnitName());

		if (oldPui != null) {
			postProcessPersistenceUnitInfo(pui, oldPui);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager#isPersistenceUnitOverrideAllowed()
	 */
	@Override
	protected boolean isPersistenceUnitOverrideAllowed() {
		return true;
	}

	void postProcessPersistenceUnitInfo(MutablePersistenceUnitInfo pui, PersistenceUnitInfo oldPui) {

		String persistenceUnitName = pui.getPersistenceUnitName();

		for (URL url : oldPui.getJarFileUrls()) {
			if (!pui.getJarFileUrls().contains(url)) {
				LOG.debug("Adding JAR file URL {} to persistence unit {}.", url, persistenceUnitName);
				pui.addJarFileUrl(url);
			}
		}

		for (String className : oldPui.getManagedClassNames()) {
			if (!pui.getManagedClassNames().contains(className)) {
				LOG.debug("Adding class {} to PersistenceUnit {}", className, persistenceUnitName);
				pui.addManagedClassName(className);
			}
		}

		for (String mappingFileName : oldPui.getMappingFileNames()) {
			if (!pui.getMappingFileNames().contains(mappingFileName)) {
				LOG.debug("Adding mapping file to persistence unit {}.", mappingFileName, persistenceUnitName);
				pui.addMappingFileName(mappingFileName);
			}
		}

		URL newUrl = pui.getPersistenceUnitRootUrl();
		URL oldUrl = oldPui.getPersistenceUnitRootUrl();

		if (oldUrl == null || newUrl == null) {
			return;
		}

		try {
			boolean rootUrlsDiffer = !newUrl.toURI().equals(oldUrl.toURI());
			boolean urlNotInJarUrls = !pui.getJarFileUrls().contains(oldUrl);

			if (rootUrlsDiffer && urlNotInJarUrls) {
				pui.addJarFileUrl(oldUrl);
			}
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}
}
