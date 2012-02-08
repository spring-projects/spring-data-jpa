/*
 * Copyright 2011-2012 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.persistence.spi.PersistenceUnitInfo;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.orm.jpa.persistenceunit.MutablePersistenceUnitInfo;

/**
 * Unit test for {@link MergingPersistenceUnitManager}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class MergingPersistenceUnitManagerUnitTests {

	@Mock
	PersistenceUnitInfo oldInfo;

	@Mock
	MutablePersistenceUnitInfo newInfo;

	@Test
	public void addsUrlFromOldPUItoNewOne() throws MalformedURLException {

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		URL jarFileUrl = new URL("file:foo/bar");

		when(oldInfo.getJarFileUrls()).thenReturn(Arrays.asList(jarFileUrl));
		manager.postProcessPersistenceUnitInfo(newInfo, oldInfo);
		verify(newInfo).addJarFileUrl(jarFileUrl);
	}

	/**
	 * @FIXME - Reactivate as soon as Spring 3.1.1 gets released
	 */
	@Test
	@Ignore
	public void mergesManagedClassesCorrectly() {

		MergingPersistenceUnitManager manager = new MergingPersistenceUnitManager();
		manager.setPersistenceXmlLocations(new String[] { "classpath:org/springframework/data/jpa/support/persistence.xml",
				"classpath:org/springframework/data/jpa/support/persistence2.xml" });
		manager.preparePersistenceUnitInfos();

		PersistenceUnitInfo info = manager.obtainPersistenceUnitInfo("pu");
		assertThat(info.getManagedClassNames().size(), is(2));
		assertThat(info.getManagedClassNames(), hasItems(User.class.getName(), Role.class.getName()));
	}
}
