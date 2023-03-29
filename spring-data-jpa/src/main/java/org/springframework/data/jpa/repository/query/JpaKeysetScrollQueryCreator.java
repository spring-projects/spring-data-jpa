/*
 * Copyright 2023 the original author or authors.
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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

/**
 * Extension to {@link JpaQueryCreator} to create queries considering {@link KeysetScrollPosition keyset scrolling}.
 *
 * @author Mark Paluch
 * @since 3.1
 */
class JpaKeysetScrollQueryCreator extends JpaQueryCreator {

	private final JpaEntityInformation<?, ?> entityInformation;
	private final KeysetScrollPosition scrollPosition;

	public JpaKeysetScrollQueryCreator(PartTree tree, ReturnedType type, CriteriaBuilder builder,
			ParameterMetadataProvider provider, JpaEntityInformation<?, ?> entityInformation,
			KeysetScrollPosition scrollPosition) {

		super(tree, type, builder, provider);

		this.entityInformation = entityInformation;
		this.scrollPosition = scrollPosition;
	}

	@Override
	protected CriteriaQuery<?> complete(@Nullable Predicate predicate, Sort sort, CriteriaQuery<?> query,
			CriteriaBuilder builder, Root<?> root) {

		KeysetScrollSpecification<Object> keysetSpec = new KeysetScrollSpecification<>(scrollPosition, sort,
				entityInformation);
		Predicate keysetPredicate = keysetSpec.createPredicate(root, builder);

		CriteriaQuery<?> queryToUse = super.complete(predicate, keysetSpec.sort(), query, builder, root);

		if (keysetPredicate != null) {
			if (queryToUse.getRestriction() != null) {
				return queryToUse.where(builder.and(queryToUse.getRestriction(), keysetPredicate));
			}
			return queryToUse.where(keysetPredicate);
		}

		return queryToUse;
	}

	@Override
	Collection<String> getRequiredSelection(Sort sort, ReturnedType returnedType) {

		Sort sortToUse = KeysetScrollSpecification.createSort(scrollPosition, sort, entityInformation);

		Set<String> selection = new LinkedHashSet<>(returnedType.getInputProperties());
		sortToUse.forEach(it -> selection.add(it.getProperty()));

		return selection;
	}
}
