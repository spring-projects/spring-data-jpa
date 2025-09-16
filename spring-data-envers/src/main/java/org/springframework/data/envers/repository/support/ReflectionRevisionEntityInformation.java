/*
 * Copyright 2012-2025 the original author or authors.
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

import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RevisionEntityInformation} that uses reflection to inspect a property annotated with {@link RevisionNumber} to
 * find out about the revision number type.
 *
 * @author Oliver Gierke
 * @author Chaedong Im
 */
public class ReflectionRevisionEntityInformation implements EnversRevisionEntityInformation {

	private final Class<?> revisionEntityClass;
	private final Class<?> revisionNumberType;
	private final String revisionTimestampFieldName;

	/**
	 * Creates a new {@link ReflectionRevisionEntityInformation} inspecting the given revision entity class.
	 *
	 * @param revisionEntityClass must not be {@literal null}.
	 */
	public ReflectionRevisionEntityInformation(Class<?> revisionEntityClass) {

		Assert.notNull(revisionEntityClass, "Revision entity type must not be null");

		AnnotationDetectionFieldCallback revisionNumberFieldCallback = new AnnotationDetectionFieldCallback(RevisionNumber.class);
		ReflectionUtils.doWithFields(revisionEntityClass, revisionNumberFieldCallback);

		AnnotationDetectionFieldCallback revisionTimestampFieldCallback = new AnnotationDetectionFieldCallback(RevisionTimestamp.class);
		ReflectionUtils.doWithFields(revisionEntityClass, revisionTimestampFieldCallback);

		this.revisionNumberType = revisionNumberFieldCallback.getRequiredType();
		this.revisionTimestampFieldName = revisionTimestampFieldCallback.getRequiredField().getName();
		this.revisionEntityClass = revisionEntityClass;
	}

	@Override
	public boolean isDefaultRevisionEntity() {
		return false;
	}

	@Override
	public Class<?> getRevisionEntityClass() {
		return this.revisionEntityClass;
	}

	@Override
	public Class<?> getRevisionNumberType() {
		return this.revisionNumberType;
	}

	@Override
	public String getRevisionTimestampPropertyName() {
		return this.revisionTimestampFieldName;
	}
}
