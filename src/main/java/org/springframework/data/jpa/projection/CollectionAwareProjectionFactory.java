/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.jpa.projection;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Map;

import org.springframework.data.projection.ProjectionInformation;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

/**
 * A {@link org.springframework.data.projection.ProjectionFactory} considering projections containing collections or
 * maps to be open.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 */
public class CollectionAwareProjectionFactory extends SpelAwareProxyProjectionFactory {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.projection.SpelAwareProxyProjectionFactory#createProjectionInformation(java.lang.Class)
	 */
	@Override
	protected ProjectionInformation createProjectionInformation(Class<?> projectionType) {
		return new CollectionAwareProjectionInformation(projectionType);
	}

	private static class CollectionAwareProjectionInformation extends SpelAwareProjectionInformation {

		CollectionAwareProjectionInformation(Class<?> projectionType) {
			super(projectionType);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.projection.SpelAwareProxyProjectionFactory.SpelAwareProjectionInformation#isInputProperty(java.beans.PropertyDescriptor)
		 */
		@Override
		protected boolean isInputProperty(PropertyDescriptor descriptor) {

			if (!super.isInputProperty(descriptor)) {
				return false;
			}

			return !(Collection.class.isAssignableFrom(descriptor.getPropertyType()) //
					|| Map.class.isAssignableFrom(descriptor.getPropertyType()));
		}
	}
}
