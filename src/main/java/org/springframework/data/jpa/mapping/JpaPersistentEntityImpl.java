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
package org.springframework.data.jpa.mapping;

import java.util.Comparator;

import org.springframework.data.annotation.Version;
import org.springframework.data.jpa.provider.ProxyIdAccessor;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.IdPropertyIdentifierAccessor;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Implementation of {@link JpaPersistentEntity}.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Michael J. Simons
 * @since 1.3
 */
class JpaPersistentEntityImpl<T> extends BasicPersistentEntity<T, JpaPersistentProperty>
		implements JpaPersistentEntity<T> {

	private static final String INVALID_VERSION_ANNOTATION = "%s is annotated with "
			+ org.springframework.data.annotation.Version.class.getName() + " but needs to use "
			+ javax.persistence.Version.class.getName() + " to trigger optimistic locking correctly!";

	private final ProxyIdAccessor proxyIdAccessor;
	private final JpaMetamodel metamodel;

	/**
	 * Creates a new {@link JpaPersistentEntityImpl} using the given {@link TypeInformation} and {@link Comparator}.
	 *
	 * @param information must not be {@literal null}.
	 * @param proxyIdAccessor must not be {@literal null}.
	 * @param metamodel must not be {@literal null}.
	 */
	public JpaPersistentEntityImpl(TypeInformation<T> information, ProxyIdAccessor proxyIdAccessor,
			JpaMetamodel metamodel) {

		super(information, null);

		Assert.notNull(proxyIdAccessor, "ProxyIdAccessor must not be null!");
		this.proxyIdAccessor = proxyIdAccessor;
		this.metamodel = metamodel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#returnPropertyIfBetterIdPropertyCandidateOrNull(org.springframework.data.mapping.PersistentProperty)
	 */
	@Override
	protected JpaPersistentProperty returnPropertyIfBetterIdPropertyCandidateOrNull(JpaPersistentProperty property) {
		return property.isIdProperty() ? property : null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#getIdentifierAccessor(java.lang.Object)
	 */
	@Override
	public IdentifierAccessor getIdentifierAccessor(Object bean) {
		return new JpaProxyAwareIdentifierAccessor(this, bean, proxyIdAccessor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.BasicPersistentEntity#verify()
	 */
	@Override
	public void verify() {

		super.verify();

		JpaPersistentProperty versionProperty = getVersionProperty();

		if (versionProperty != null && versionProperty.isAnnotationPresent(Version.class)) {
			throw new IllegalArgumentException(String.format(INVALID_VERSION_ANNOTATION, versionProperty));
		}
	}

	JpaMetamodel getMetamodel() {
		return metamodel;
	}

	/**
	 * {@link IdentifierAccessor} that tries to use a {@link ProxyIdAccessor} for id access to potentially avoid the
	 * initialization of JPA proxies. We're falling back to the default behavior of {@link IdPropertyIdentifierAccessor}
	 * if that's not possible.
	 *
	 * @author Oliver Gierke
	 */
	private static class JpaProxyAwareIdentifierAccessor extends IdPropertyIdentifierAccessor {

		private final Object bean;
		private final ProxyIdAccessor proxyIdAccessor;

		/**
		 * Creates a new {@link JpaProxyAwareIdentifierAccessor} for the given {@link JpaPersistentEntity}, target bean and
		 * {@link ProxyIdAccessor}.
		 *
		 * @param entity must not be {@literal null}.
		 * @param bean must not be {@literal null}.
		 * @param proxyIdAccessor must not be {@literal null}.
		 */
		JpaProxyAwareIdentifierAccessor(JpaPersistentEntity<?> entity, Object bean, ProxyIdAccessor proxyIdAccessor) {

			super(entity, bean);

			Assert.notNull(proxyIdAccessor, "Proxy identifier accessor must not be null!");

			this.proxyIdAccessor = proxyIdAccessor;
			this.bean = bean;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.mapping.IdentifierAccessor#getIdentifier()
		 */
		@Override
		public Object getIdentifier() {

			return proxyIdAccessor.shouldUseAccessorFor(bean) //
					? proxyIdAccessor.getIdentifierFrom(bean)//
					: super.getIdentifier();
		}
	}
}
