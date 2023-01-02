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

import org.hibernate.envers.RevisionNumber;

import org.springframework.data.repository.history.support.RevisionEntityInformation;
import org.springframework.data.util.AnnotationDetectionFieldCallback;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RevisionEntityInformation} that uses reflection to inspect a property annotated with {@link RevisionNumber} to
 * find out about the revision number type.
 *
 * @author Oliver Gierke
 */
public class ReflectionRevisionEntityInformation implements RevisionEntityInformation {

	private final Class<?> revisionEntityClass;
	private final Class<?> revisionNumberType;

	/**
	 * Creates a new {@link ReflectionRevisionEntityInformation} inspecting the given revision entity class.
	 *
	 * @param revisionEntityClass must not be {@literal null}.
	 */
	public ReflectionRevisionEntityInformation(Class<?> revisionEntityClass) {

		Assert.notNull(revisionEntityClass, "Revision entity type must not be null");

		AnnotationDetectionFieldCallback fieldCallback = new AnnotationDetectionFieldCallback(RevisionNumber.class);
		ReflectionUtils.doWithFields(revisionEntityClass, fieldCallback);

		this.revisionNumberType = fieldCallback.getRequiredType();
		this.revisionEntityClass = revisionEntityClass;

	}

	public boolean isDefaultRevisionEntity() {
		return false;
	}

	public Class<?> getRevisionEntityClass() {
		return this.revisionEntityClass;
	}

	public Class<?> getRevisionNumberType() {
		return this.revisionNumberType;
	}
}
