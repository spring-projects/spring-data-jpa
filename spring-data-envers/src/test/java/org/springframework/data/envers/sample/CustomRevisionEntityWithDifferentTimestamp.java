/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.envers.sample;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * Custom revision entity with a non-standard timestamp field name to test dynamic timestamp property detection.
 *
 * @author Chaedong Im
 */
@Entity
@RevisionEntity
public class CustomRevisionEntityWithDifferentTimestamp {

	@Id @GeneratedValue @RevisionNumber 
	private int revisionId;
	
	@RevisionTimestamp
	private long myCustomTimestamp;  // Non-standard field name
	
	public int getRevisionId() {
		return revisionId;
	}
	
	public void setRevisionId(int revisionId) {
		this.revisionId = revisionId;
	}
	
	public long getMyCustomTimestamp() {
		return myCustomTimestamp;
	}
	
	public void setMyCustomTimestamp(long myCustomTimestamp) {
		this.myCustomTimestamp = myCustomTimestamp;
	}
}
