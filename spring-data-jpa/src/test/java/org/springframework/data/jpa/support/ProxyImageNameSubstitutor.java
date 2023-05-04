/*
 * Copyright 2015-2023 the original author or authors.
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

import java.util.List;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

/**
 * An {@link ImageNameSubstitutor} only used on CI servers to leverage internal proxy solution, that needs to vary the
 * prefix based on which container image is needed.
 * 
 * @author Greg Turnquist
 */
public class ProxyImageNameSubstitutor extends ImageNameSubstitutor {

	private static final List<String> NAMES_TO_PROXY_PREFIX = List.of("ryuk");

	private static final List<String> NAMES_TO_LIBRARY_PROXY_PREFIX = List.of("mysql", "postgres");

	private static final String PROXY_PREFIX = "harbor-repo.vmware.com/dockerhub-proxy-cache/";

	private static final String LIBRARY_PROXY_PREFIX = PROXY_PREFIX + "library/";

	@Override
	public DockerImageName apply(DockerImageName dockerImageName) {

		if (NAMES_TO_PROXY_PREFIX.stream().anyMatch(s -> dockerImageName.asCanonicalNameString().contains(s))) {
			return DockerImageName.parse(applyProxyPrefix(dockerImageName.asCanonicalNameString()));
		}

		if (NAMES_TO_LIBRARY_PROXY_PREFIX.stream().anyMatch(s -> dockerImageName.asCanonicalNameString().contains(s))) {
			return DockerImageName.parse(applyProxyAndLibraryPrefix(dockerImageName.asCanonicalNameString()));
		}

		return dockerImageName;
	}

	@Override
	protected String getDescription() {
		return "Spring Data Proxy Image Name Substitutor";
	}

	/**
	 * Apply a non-library-based prefix.
	 */
	private static String applyProxyPrefix(String imageName) {
		return PROXY_PREFIX + imageName;
	}

	/**
	 * Apply a library based prefix.
	 */
	private static String applyProxyAndLibraryPrefix(String imageName) {
		return LIBRARY_PROXY_PREFIX + imageName;
	}
}
