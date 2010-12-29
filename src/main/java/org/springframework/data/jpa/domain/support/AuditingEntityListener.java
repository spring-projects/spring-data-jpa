/*
 * Copyright 2008-2010 the original author or authors.
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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.domain.Auditable;
import org.springframework.data.domain.AuditorAware;


/**
 * JPA entity listener to capture auditing information on persiting and updating
 * entities. To get this one flying be sure you configure it as entity listener
 * in your {@code orm.xml} as follows:
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
 */
@Configurable
public class AuditingEntityListener<T> implements InitializingBean {

    private static final Logger LOG = LoggerFactory
            .getLogger(AuditingEntityListener.class);

    private AuditorAware<T> auditorAware;

    private boolean dateTimeForNow = true;
    private boolean modifyOnCreation = true;


    /**
     * Setter to inject a {@code AuditorAware} component to retrieve the current
     * auditor.
     * 
     * @param auditorAware the auditorAware to set
     */
    public void setAuditorAware(final AuditorAware<T> auditorAware) {

        this.auditorAware = auditorAware;
    }


    /**
     * Setter do determine if {@link Auditable#setCreatedDate(DateTime)} and
     * {@link Auditable#setLastModifiedDate(DateTime)} shall be filled with the
     * current Java time. Defaults to {@code true}. One might set this to
     * {@code false} to use database features to set entity time.
     * 
     * @param dateTimeForNow the dateTimeForNow to set
     */
    public void setDateTimeForNow(boolean dateTimeForNow) {

        this.dateTimeForNow = dateTimeForNow;
    }


    /**
     * Set this to false if you want to treat entity creation as modification
     * and thus set the current date as modification date, too. Defaults to
     * {@code true}.
     * 
     * @param modifyOnCreation if modification information shall be set on
     *            creation, too
     */
    public void setModifyOnCreation(final boolean modifyOnCreation) {

        this.modifyOnCreation = modifyOnCreation;
    }


    /**
     * Sets modification and creation date and auditor on the target object in
     * case it implements {@link Auditable}.
     * 
     * @param target
     */
    @PrePersist
    @PreUpdate
    public void touch(Object target) {

        if (!(target instanceof Auditable)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Auditable<T, ?> auditable = (Auditable<T, ?>) target;

        T auditor = touchAuditor(auditable);
        DateTime now = dateTimeForNow ? touchDate(auditable) : null;

        Object defaultedNow = now == null ? "not set" : now;
        Object defaultedAuditor = auditor == null ? "unknown" : auditor;

        LOG.debug("Touched {} - Last modification at {} by {}", new Object[] {
                auditable, defaultedNow, defaultedAuditor });
    }


    /**
     * Sets modifying and creating auditioner. Creating auditioner is only set
     * on new auditables.
     * 
     * @param auditable
     * @return
     */
    private T touchAuditor(final Auditable<T, ?> auditable) {

        if (null == auditorAware) {
            return null;
        }

        T auditor = auditorAware.getCurrentAuditor();

        if (auditable.isNew()) {

            auditable.setCreatedBy(auditor);

            if (!modifyOnCreation) {
                return auditor;
            }
        }

        auditable.setLastModifiedBy(auditor);

        return auditor;
    }


    /**
     * Touches the auditable regarding modification and creation date. Creation
     * date is only set on new auditables.
     * 
     * @param auditable
     * @return
     */
    private DateTime touchDate(final Auditable<T, ?> auditable) {

        DateTime now = new DateTime();

        if (auditable.isNew()) {
            auditable.setCreatedDate(now);

            if (!modifyOnCreation) {
                return now;
            }
        }

        auditable.setLastModifiedDate(now);

        return now;
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() {

        if (auditorAware == null) {
            LOG.debug("No AuditorAware set! Auditing will not be applied!");
        }
    }
}
