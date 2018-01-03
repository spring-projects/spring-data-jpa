/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.data.jpa.domain.sample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.springframework.data.domain.Persistable;

/**
 * Sample entity using {@link IdClass} annotation to demarcate ids.
 *
 * @author Oliver Gierke
 */
@Entity
@IdClass(PersistableWithIdClassPK.class)
public class PersistableWithIdClass implements Persistable<PersistableWithIdClassPK> {

	private static final long serialVersionUID = 1L;

	@Id
	Long first;

	@Id
	Long second;

	private boolean isNew;

	protected PersistableWithIdClass() {
		this.isNew = true;
	}

	public PersistableWithIdClass(Long first, Long second) {
		this.first = first;
		this.second = second;
		this.isNew = true;
	}

	/**
	 * @return the first
	 */
	public Long getFirst() {
		return first;
	}

	/**
	 * @return the second
	 */
	public Long getSecond() {
		return second;
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#getId()
	 */
	public PersistableWithIdClassPK getId() {
		return new PersistableWithIdClassPK(first, second);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.domain.Persistable#isNew()
	 */
	public boolean isNew() {
		return this.isNew;
	}

	public void setNotNew() {
		this.isNew = false;
	}
}
