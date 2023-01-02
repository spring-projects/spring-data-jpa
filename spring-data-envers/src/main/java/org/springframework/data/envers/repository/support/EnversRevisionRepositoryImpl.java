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

import static org.springframework.data.history.RevisionMetadata.RevisionType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.order.AuditOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.AnnotationRevisionMetadata;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionMetadata;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Repository implementation using Hibernate Envers to implement revision specific query methods.
 *
 * @author Oliver Gierke
 * @author Philipp Huegelmeyer
 * @author Michael Igler
 * @author Jens Schauder
 * @author Julien Millau
 * @author Mark Paluch
 * @author Sander Bylemans
 */
@Transactional(readOnly = true)
public class EnversRevisionRepositoryImpl<T, ID, N extends Number & Comparable<N>>
		implements RevisionRepository<T, ID, N> {

	private final EntityInformation<T, ?> entityInformation;
	private final EntityManager entityManager;

	/**
	 * Creates a new {@link EnversRevisionRepositoryImpl} using the given {@link JpaEntityInformation},
	 * {@link RevisionEntityInformation} and {@link EntityManager}.
	 *
	 * @param entityInformation must not be {@literal null}.
	 * @param revisionEntityInformation must not be {@literal null}.
	 * @param entityManager must not be {@literal null}.
	 */
	public EnversRevisionRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
			RevisionEntityInformation revisionEntityInformation, EntityManager entityManager) {

		Assert.notNull(revisionEntityInformation, "RevisionEntityInformation must not be null");

		this.entityInformation = entityInformation;
		this.entityManager = entityManager;
	}

	@SuppressWarnings("unchecked")
	public Optional<Revision<N, T>> findLastChangeRevision(ID id) {

		List<Object[]> singleResult = createBaseQuery(id) //
				.addOrder(AuditEntity.revisionProperty("timestamp").desc()) //
				.setMaxResults(1) //
				.getResultList();

		Assert.state(singleResult.size() <= 1, "We expect at most one result");

		if (singleResult.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(createRevision(new QueryResult<>(singleResult.get(0))));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Revision<N, T>> findRevision(ID id, N revisionNumber) {

		Assert.notNull(id, "Identifier must not be null");
		Assert.notNull(revisionNumber, "Revision number must not be null");

		List<Object[]> singleResult = (List<Object[]>) createBaseQuery(id) //
				.add(AuditEntity.revisionNumber().eq(revisionNumber)) //
				.getResultList();

		Assert.state(singleResult.size() <= 1, "We expect at most one result");

		if (singleResult.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(createRevision(new QueryResult<>(singleResult.get(0))));
	}

	@SuppressWarnings("unchecked")
	public Revisions<N, T> findRevisions(ID id) {

		List<Object[]> resultList = createBaseQuery(id).getResultList();
		List<Revision<N, T>> revisionList = new ArrayList<>(resultList.size());

		for (Object[] objects : resultList) {
			revisionList.add(createRevision(new QueryResult<>(objects)));
		}

		return Revisions.of(revisionList);
	}

	@SuppressWarnings("unchecked")
	public Page<Revision<N, T>> findRevisions(ID id, Pageable pageable) {

		AuditOrder sorting = RevisionSort.getRevisionDirection(pageable.getSort()).isDescending() //
				? AuditEntity.revisionNumber().desc() //
				: AuditEntity.revisionNumber().asc();

		List<Object[]> resultList = createBaseQuery(id) //
				.addOrder(sorting) //
				.setFirstResult((int) pageable.getOffset()) //
				.setMaxResults(pageable.getPageSize()) //
				.getResultList();

		Long count = (Long) createBaseQuery(id) //
				.addProjection(AuditEntity.revisionNumber().count()).getSingleResult();

		List<Revision<N, T>> revisions = new ArrayList<>();

		for (Object[] singleResult : resultList) {
			revisions.add(createRevision(new QueryResult<>(singleResult)));
		}

		return new PageImpl<>(revisions, pageable, count);
	}

	private AuditQuery createBaseQuery(ID id) {

		Class<T> type = entityInformation.getJavaType();
		AuditReader reader = AuditReaderFactory.get(entityManager);

		return reader.createQuery() //
				.forRevisionsOfEntity(type, false, true) //
				.add(AuditEntity.id().eq(id));
	}

	@SuppressWarnings("unchecked")
	private Revision<N, T> createRevision(QueryResult<T> queryResult) {
		return Revision.of((RevisionMetadata<N>) queryResult.createRevisionMetadata(), queryResult.entity);
	}

	@SuppressWarnings("unchecked")
	static class QueryResult<T> {

		private final T entity;
		private final Object metadata;
		private final RevisionMetadata.RevisionType revisionType;

		QueryResult(Object[] data) {

			Assert.notNull(data, "Data must not be null");
			Assert.isTrue( //
					data.length == 3, //
					() -> String.format("Data must have length three, but has length %d", data.length));
			Assert.isTrue( //
					data[2] instanceof RevisionType, //
					() -> String.format("The third array element must be of type Revision type, but is of type %s",
							data[2].getClass()));

			entity = (T) data[0];
			metadata = data[1];
			revisionType = convertRevisionType((RevisionType) data[2]);
		}

		RevisionMetadata<?> createRevisionMetadata() {

			return metadata instanceof DefaultRevisionEntity //
					? new DefaultRevisionMetadata((DefaultRevisionEntity) metadata, revisionType) //
					: new AnnotationRevisionMetadata<>(Hibernate.unproxy(metadata), RevisionNumber.class, RevisionTimestamp.class,
							revisionType);
		}

		private static RevisionMetadata.RevisionType convertRevisionType(RevisionType datum) {

			switch (datum) {

				case ADD:
					return INSERT;
				case MOD:
					return UPDATE;
				case DEL:
					return DELETE;
				default:
					return UNKNOWN;
			}
		}
	}

}
