/*
 * Copyright 2023-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.KeysetScrollPosition;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Extension to {@link JpaQueryCreator} to create queries considering {@link KeysetScrollPosition keyset scrolling}.
 *
 * @author Mark Paluch
 * @since 3.1
 */
class JpaKeysetScrollQueryCreator extends JpaQueryCreator {

	private final JpaEntityInformation<?, ?> entityInformation;
	private final KeysetScrollPosition scrollPosition;
	private final ParameterMetadataProvider provider;
	private final List<ParameterBinding> syntheticBindings = new ArrayList<>();

	public JpaKeysetScrollQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, JpaEntityInformation<?, ?> entityInformation, KeysetScrollPosition scrollPosition,
			EntityManager em) {

		super(tree, type, provider, templates, em);

		this.entityInformation = entityInformation;
		this.scrollPosition = scrollPosition;
		this.provider = provider;
	}

	@Override
	public List<ParameterBinding> getBindings() {

		List<ParameterBinding> partTreeBindings = super.getBindings();
		List<ParameterBinding> bindings = new ArrayList<>(partTreeBindings.size() + this.syntheticBindings.size());
		bindings.addAll(partTreeBindings);
		bindings.addAll(this.syntheticBindings);

		return bindings;
	}

	@Override
	protected JpqlQueryBuilder.AbstractJpqlQuery createQuery(JpqlQueryBuilder.@Nullable Predicate predicate, Sort sort) {

		KeysetScrollSpecification<Object> keysetSpec = new KeysetScrollSpecification<>(scrollPosition, sort,
				entityInformation);

		JpqlQueryBuilder.Select query = buildQuery(keysetSpec.sort());

		AtomicInteger counter = new AtomicInteger(provider.getBindings().size());
		JpqlQueryBuilder.Predicate keysetPredicate = keysetSpec.createJpqlPredicate(getFrom(), getEntity(), value -> {

			syntheticBindings.add(provider.nextSynthetic(value, scrollPosition));
			return placeholder(counter.incrementAndGet());
		});
		JpqlQueryBuilder.Predicate predicateToUse = getPredicate(predicate, keysetPredicate);

		if (predicateToUse != null) {
			return query.where(predicateToUse);
		}

		return query;
	}


	private static JpqlQueryBuilder.@Nullable Predicate getPredicate(JpqlQueryBuilder.@Nullable Predicate predicate,
			JpqlQueryBuilder.@Nullable Predicate keysetPredicate) {

		if (keysetPredicate != null) {
			if (predicate != null) {
				return predicate.nest().and(keysetPredicate.nest());
			} else {
				return keysetPredicate;
			}
		}

		return predicate;
	}

	@Override
	Collection<String> getRequiredSelection(Sort sort, ReturnedType returnedType) {

		Sort sortToUse = KeysetScrollSpecification.createSort(scrollPosition, sort, entityInformation);

		return KeysetScrollDelegate.getProjectionInputProperties(entityInformation, returnedType.getInputProperties(),
				sortToUse);
	}
}
