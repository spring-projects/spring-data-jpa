/*
 * Copyright 2008-2013 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Special {@link JpaQueryCreator} that creates a count projecting query.
 * 
 * @author Oliver Gierke
 */
public class JpaCountQueryCreator extends JpaQueryCreator {

	/**
	 * Creates a new {@link JpaCountQueryCreator}.
	 * 
	 * @param tree
	 * @param domainClass
	 * @param parameters
	 * @param em
	 */
	public JpaCountQueryCreator(PartTree tree, Class<?> domainClass, CriteriaBuilder builder,
			ParameterMetadataProvider provider) {
		super(tree, domainClass, builder, provider);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.JpaQueryCreator#complete(javax.persistence.criteria.Predicate, org.springframework.data.domain.Sort, javax.persistence.criteria.CriteriaQuery, javax.persistence.criteria.CriteriaBuilder, javax.persistence.criteria.Root)
	 */
	@Override
	protected CriteriaQuery<Object> complete(Predicate predicate, Sort sort, CriteriaQuery<Object> query,
			CriteriaBuilder builder, Root<?> root) {

		CriteriaQuery<Object> select = query.select(builder.count(root));
		return predicate == null ? select : select.where(predicate);
	}
}
