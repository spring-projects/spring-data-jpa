/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Metamodel;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Special {@link JpaQueryCreator} that creates a count projecting query.
 *
 * @author Oliver Gierke
 * @author Marc Lefrançois
 * @author Mark Paluch
 * @author Greg Turnquist
 */
public class JpaCountQueryCreator extends JpaQueryCreator {

	private final boolean distinct;
	private final ReturnedType returnedType;

	/**
	 * Creates a new {@link JpaCountQueryCreator}
	 *
	 * @param tree
	 * @param returnedType
	 * @param provider
	 * @param templates
	 * @param em
	 */
	public JpaCountQueryCreator(PartTree tree, ReturnedType returnedType, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, EntityManager em) {

		super(tree, returnedType, provider, templates, em);

		this.distinct = tree.isDistinct();
		this.returnedType = returnedType;
	}

	/**
	 * Creates a new {@link JpaCountQueryCreator}
	 *
	 * @param tree
	 * @param returnedType
	 * @param provider
	 * @param templates
	 * @param metamodel
	 */
	public JpaCountQueryCreator(PartTree tree, ReturnedType returnedType, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, Metamodel metamodel) {

		super(tree, returnedType, provider, templates, metamodel);

		this.distinct = tree.isDistinct();
		this.returnedType = returnedType;
	}

	@Override
	protected JpqlQueryBuilder.Select buildQuery(Sort sort) {
		JpqlQueryBuilder.SelectStep selectStep = JpqlQueryBuilder.selectFrom(returnedType.getDomainType());
		if (this.distinct) {
			selectStep = selectStep.distinct();
		}

		return selectStep.count();
	}
}
