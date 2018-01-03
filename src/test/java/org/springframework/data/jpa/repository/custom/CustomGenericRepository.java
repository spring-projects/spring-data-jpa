/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.custom;

import java.io.Serializable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.CrudRepository;

/**
 * Extension of {@link CrudRepository} to be added on a custom repository base class. This tests the facility to
 * implement custom base class functionality for all repository instances derived from this interface and implementation
 * base class.
 *
 * @author Oliver Gierke
 */
@NoRepositoryBean
public interface CustomGenericRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

	/**
	 * Custom sample method.
	 *
	 * @param id
	 * @return
	 */
	T customMethod(ID id);
}
