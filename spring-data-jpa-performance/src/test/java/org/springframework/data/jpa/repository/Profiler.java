/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository;

import java.lang.reflect.Method;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.repository.query.StringQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Mark Paluch
 */
public class Profiler {

	public static void main(String[] args) throws Exception {

		DefaultRepositoryMetadata art = new DefaultRepositoryMetadata(PersonRepository.class);
		Method method = PersonRepository.class.getMethod("findAllWithAnnotatedQueryByFirstname", String.class, Sort.class);
		ProjectionFactory projectionFactory = new SpelAwareProxyProjectionFactory();

		System.out.println("Ready. Waiting 10sec");
		Thread.sleep(10000);

		System.out.println("Go!");


		while (true) {

			JpaQueryMethod queryMethod = new JpaQueryMethod(method, art, projectionFactory, PersistenceProvider.HIBERNATE);
			StringQuery stringQuery = new StringQuery(queryMethod.getRequiredAnnotatedQuery(), false);
		}

	}
}
