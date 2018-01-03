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
package org.springframework.data.jpa.repository;

import java.sql.Types;

import org.hibernate.dialect.HSQLDialect;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Fix for missing type declarations for HSQL.
 *
 * @see <a href="http://www.codesmell.org/blog/2008/12/hibernate-hsql-native-queries-and-booleans/">http://www.codesmell.org/blog/2008/12/hibernate-hsql-native-queries-and-booleans/</a>
 * @author Oliver Gierke
 */
public class CustomHsqlHibernateJpaVendorAdaptor extends HibernateJpaVendorAdapter {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter#determineDatabaseDialectClass(org.springframework.orm.jpa.vendor.Database)
	 */
	@Override
	protected Class<?> determineDatabaseDialectClass(Database database) {

		if (Database.HSQL.equals(database)) {
			return CustomHsqlDialect.class;
		}

		return super.determineDatabaseDialectClass(database);
	}

	public static class CustomHsqlDialect extends HSQLDialect {

		public CustomHsqlDialect() {
			registerColumnType(Types.BOOLEAN, "boolean");
			registerHibernateType(Types.BOOLEAN, "boolean");
		}
	}
}
