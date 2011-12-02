/*
 * Copyright 2008-2011 the original author or authors.
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
import java.util.Date;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.joda.time.DateTime;
import org.springframework.data.domain.Auditable;

/**
 * Abstract base class for auditable entities. Stores the audition values in persistent fields.
 * 
 * @author Oliver Gierke
 * @param <U> the auditing type. Typically some kind of user.
 * @param <PK> the type of the auditing type's idenifier
 */
@MappedSuperclass
public abstract class AbstractAuditable<U, PK extends Serializable> extends AbstractPersistable<PK> implements
		Auditable<U, PK> {

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
	public U getCreatedBy() {

		return createdBy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setCreatedBy(java.lang.Object)
	 */
	public void setCreatedBy(final U createdBy) {

		this.createdBy = createdBy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getCreatedDate()
	 */
	public DateTime getCreatedDate() {

		return null == createdDate ? null : new DateTime(createdDate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setCreatedDate(org.joda.time
	 * .DateTime)
	 */
	public void setCreatedDate(final DateTime createdDate) {

		this.createdDate = null == createdDate ? null : createdDate.toDate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getLastModifiedBy()
	 */
	public U getLastModifiedBy() {

		return lastModifiedBy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setLastModifiedBy(java.lang
	 * .Object)
	 */
	public void setLastModifiedBy(final U lastModifiedBy) {

		this.lastModifiedBy = lastModifiedBy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.domain.Auditable#getLastModifiedDate()
	 */
	public DateTime getLastModifiedDate() {

		return null == lastModifiedDate ? null : new DateTime(lastModifiedDate);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.domain.Auditable#setLastModifiedDate(org.joda
	 * .time.DateTime)
	 */
	public void setLastModifiedDate(final DateTime lastModifiedDate) {

		this.lastModifiedDate = null == lastModifiedDate ? null : lastModifiedDate.toDate();
	}
}
