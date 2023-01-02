/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.data.envers.repository.support;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.history.RevisionRepository;

/**
 * Convenience interface to allow pulling in {@link JpaRepository} and {@link RevisionRepository} functionality in one
 * go.
 *
 * @author Oliver Gierke
 * @author Michael Igler
 * @deprecated since 1.1, in favor of simply extending {@link RevisionRepository}.
 */
@Deprecated
@NoRepositoryBean
public interface EnversRevisionRepository<T, ID extends Serializable, N extends Number & Comparable<N>>
		extends RevisionRepository<T, ID, N>, JpaRepository<T, ID> {

}
