/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.data.jpa.repository.sample;

import org.springframework.data.jpa.domain.sample.ItemSite;
import org.springframework.data.jpa.domain.sample.ItemSiteId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Mark Paluch
 * @see DATAJPA-413
 * @see Final JPA 2.1 Specification 2.4.1.3 Derived Identities Example 2
 */
public interface ItemSiteRepository extends JpaRepository<ItemSite, ItemSiteId> {}
