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
package org.springframework.data.jpa.repository;

import org.hibernate.dialect.HSQLDialect;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Fix for missing type declarations for HSQL.
 *
 * @see <a href=
 *      "https://www.codesmell.org/blog/2008/12/hibernate-hsql-native-queries-and-booleans/">https://www.codesmell.org/blog/2008/12/hibernate-hsql-native-queries-and-booleans/</a>
 * @author Oliver Gierke
 * @deprecated since 3.0 without replacement as it's not needed anymore.
 */
@Deprecated
public class CustomHsqlHibernateJpaVendorAdaptor extends HibernateJpaVendorAdapter {

	@Override
	protected Class<?> determineDatabaseDialectClass(Database database) {
		return super.determineDatabaseDialectClass(database);
	}

	/**
	 * @deprecated since 3.0 without replacement as it's not needed anymore.
	 */
	@Deprecated
	public static class CustomHsqlDialect extends HSQLDialect {}
}
