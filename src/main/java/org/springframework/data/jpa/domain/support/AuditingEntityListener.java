/*
 * Copyright 2008-2012 the original author or authors.
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
package org.springframework.data.jpa.domain.support;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.aspectj.AnnotationBeanConfigurerAspect;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.Auditable;

/**
 * JPA entity listener to capture auditing information on persiting and updating entities. To get this one flying be
 * sure you configure it as entity listener in your {@code orm.xml} as follows:
 * 
 * <pre>
 * &lt;persistence-unit-metadata&gt;
 *     &lt;persistence-unit-defaults&gt;
 *         &lt;entity-listeners&gt;
 *             &lt;entity-listener class="org.springframework.data.jpa.domain.auditing.support.AuditingEntityListener" /&gt;
 *         &lt;/entity-listeners&gt;
 *     &lt;/persistence-unit-defaults&gt;
 * &lt;/persistence-unit-metadata&gt;
 * </pre>
 * 
 * After that it's just a matter of activating auditing in your Spring config:
 * 
 * <pre>
 * &lt;jpa:auditing auditor-aware-ref="yourAuditorAwarebean" /&gt;
 * </pre>
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@Configurable
public class AuditingEntityListener<T> {

	private AuditingHandler<T> handler;

	/**
	 * Creates an {@link AuditingEntityListener} that auto-wires itself according to the appropriate spring bean
	 * configuration.
	 */
	public AuditingEntityListener() {
		AnnotationBeanConfigurerAspect.aspectOf().configureBean(this);
	}

	/**
	 * @param auditingHandler the handler to set
	 */
	public void setAuditingHandler(AuditingHandler<T> auditingHandler) {
		this.handler = auditingHandler;
	}

	/**
	 * Sets modification and creation date and auditor on the target object in case it implements {@link Auditable} on
	 * persist events.
	 * 
	 * @param target
	 */
	@PrePersist
	public void touchForCreate(Object target) {
		if (handler != null) {
			handler.markCreated(target);
		}
	}

	/**
	 * Sets modification and creation date and auditor on the target object in case it implements {@link Auditable} on
	 * update events.
	 * 
	 * @param target
	 */
	@PreUpdate
	public void touchForUpdate(Object target) {
		if (handler != null) {
			handler.markModified(target);
		}
	}
}
