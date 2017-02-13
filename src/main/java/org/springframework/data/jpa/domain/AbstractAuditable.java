/*
 * Copyright 2008-2017 the original author or authors.
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
package org.springframework.data.jpa.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.domain.Auditable;

/**
 * Abstract base class for auditable entities. Stores the audition values in persistent fields.
 * 
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @param <U> the auditing type. Typically some kind of user.
 * @param <PK> the type of the auditing type's idenifier
 */
@MappedSuperclass
public abstract class AbstractAuditable<U, PK extends Serializable> extends AbstractPersistable<PK> implements
		Auditable<U, PK, LocalDateTime> {

	private static final long serialVersionUID = 141481953116476081L;

	@ManyToOne
	private U createdBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date createdDate;

	@ManyToOne
	private U lastModifiedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date lastModifiedDate;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getCreatedBy()
	 */
	public Optional<U> getCreatedBy() {

		return Optional.ofNullable(createdBy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setCreatedBy(java.lang.Object)
	 */
	public void setCreatedBy(final Optional<? extends U> createdBy) {

		this.createdBy = createdBy.orElse(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getCreatedDate()
	 */
	@Override
	public Optional<LocalDateTime> getCreatedDate() {

		return null == createdDate ? Optional.empty() : Optional.of(LocalDateTime.ofInstant(createdDate.toInstant(), ZoneId.systemDefault()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setCreatedDate(org.joda.time
	 * .DateTime)
	 */
	public void setCreatedDate(Optional<? extends LocalDateTime> createdDate) {

		this.createdDate = createdDate.map(d -> Date.from(d.atZone(ZoneId.systemDefault()).toInstant())).orElse(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getLastModifiedBy()
	 */
	public Optional<U> getLastModifiedBy() {

		return Optional.ofNullable(lastModifiedBy);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setLastModifiedBy(java.lang
	 * .Object)
	 */
	public void setLastModifiedBy(final Optional<? extends U> lastModifiedBy) {

		this.lastModifiedBy = lastModifiedBy.orElse(null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getLastModifiedDate()
	 */
	public Optional<LocalDateTime> getLastModifiedDate() {

		return null == lastModifiedDate ? Optional.empty() : Optional.of(LocalDateTime.ofInstant(lastModifiedDate.toInstant(), ZoneId.systemDefault()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setLastModifiedDate(org.joda
	 * .time.DateTime)
	 */
	public void setLastModifiedDate(Optional<? extends LocalDateTime> lastModifiedDate) {

		this.lastModifiedDate = lastModifiedDate.map(d -> Date.from(d.atZone(ZoneId.systemDefault()).toInstant())).orElse(null);
	}
}
