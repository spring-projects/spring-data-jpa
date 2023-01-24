/*
 * Copyright 2011-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.spi.PersistenceUnitInfo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

/**
 * Unit test for {@link MergingPersistenceUnitManager}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MergingPersistenceUnitManagerUnitTests {

	@Mock PersistenceUnitInfo oldInfo;

	@Mock MutablePersistenceUnitInfo newInfo;

	@Test
	void addsUrlFromOldPUItoNewOne() throws MalformedURLException {

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		URL jarFileUrl = new URL("file:foo/bar");

		when(oldInfo.getJarFileUrls()).thenReturn(Arrays.asList(jarFileUrl));
		manager.postProcessPersistenceUnitInfo(newInfo, oldInfo);
		verify(newInfo).addJarFileUrl(jarFileUrl);
	}

	@Test
	void mergesManagedClassesCorrectly() {

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		manager.setPersistenceXmlLocations("classpath:org/springframework/data/jpa/support/persistence.xml",
				"classpath:org/springframework/data/jpa/support/persistence2.xml");
		manager.preparePersistenceUnitInfos();

		PersistenceUnitInfo info = manager.obtainPersistenceUnitInfo("pu");
		assertThat(info.getManagedClassNames()).hasSize(2);
		assertThat(info.getManagedClassNames()).contains(User.class.getName(), Role.class.getName());

		assertThat(info.getMappingFileNames()).hasSize(2);
		assertThat(info.getMappingFileNames()).contains("foo.xml", "bar.xml");
	}

	@Test
	void addsOldPersistenceUnitRootUrlIfDifferentFromNewOne() throws MalformedURLException {

		MutablePersistenceUnitInfo newInfo = new MutablePersistenceUnitInfo();
		newInfo.setPersistenceUnitRootUrl(new URL("file:bar"));

		when(oldInfo.getPersistenceUnitRootUrl()).thenReturn(new URL("file:/foo"));

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		manager.postProcessPersistenceUnitInfo(newInfo, oldInfo);

		assertThat(newInfo.getJarFileUrls()).hasSize(1);
		assertThat(newInfo.getJarFileUrls()).contains(oldInfo.getPersistenceUnitRootUrl());
	}

	@Test
	void doesNotAddNewPuRootUrlIfNull() throws MalformedURLException {

		MutablePersistenceUnitInfo newInfo = new MutablePersistenceUnitInfo();

		when(oldInfo.getPersistenceUnitRootUrl()).thenReturn(new URL("file:/foo"));

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		manager.postProcessPersistenceUnitInfo(newInfo, oldInfo);

		assertThat(newInfo.getJarFileUrls()).isEmpty();
	}

	@Test
	void doesNotAddNewPuRootUrlIfAlreadyOnTheListOfJarFileUrls() throws MalformedURLException {

		when(oldInfo.getPersistenceUnitRootUrl()).thenReturn(new URL("file:foo"));

		MutablePersistenceUnitInfo newInfo = new MutablePersistenceUnitInfo();
		newInfo.setPersistenceUnitRootUrl(new URL("file:bar"));
		newInfo.addJarFileUrl(oldInfo.getPersistenceUnitRootUrl());

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		manager.postProcessPersistenceUnitInfo(newInfo, oldInfo);

		assertThat(newInfo.getJarFileUrls()).hasSize(1);
		assertThat(newInfo.getJarFileUrls()).contains(oldInfo.getPersistenceUnitRootUrl());
	}
}
