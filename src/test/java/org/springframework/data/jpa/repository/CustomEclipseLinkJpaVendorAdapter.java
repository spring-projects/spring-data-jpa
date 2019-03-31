/*
 * Copyright 2015-2019 the original author or authors.
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

import org.eclipse.persistence.internal.databaseaccess.DatabasePlatform;
import org.eclipse.persistence.platform.database.HSQLPlatform;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

/**
 * Custom {@link EclipseLinkJpaVendorAdapter} to customize the {@link DatabasePlatform} to be sued with EclipseLink to
 * work around a bug in stored procedure execution on HSQLDB.
 *
 * @author Oliver Gierke
 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=467072">https://bugs.eclipse.org/bugs/show_bug.cgi?id=467072</a>
 */
public class CustomEclipseLinkJpaVendorAdapter extends EclipseLinkJpaVendorAdapter {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter#determineTargetDatabaseName(org.springframework.orm.jpa.vendor.Database)
	 */
	@Override
	protected String determineTargetDatabaseName(Database database) {

		if (Database.HSQL.equals(database)) {
			return EclipseLinkHsqlPlatform.class.getName();
		}

		return super.determineTargetDatabaseName(database);
	}

	/**
	 * Workaround {@link HSQLPlatform} to make sure EclipseLink uses the right syntax to call stored procedures on HSQL.
	 *
	 * @author Oliver Gierke
	 * @see <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=467072">https://bugs.eclipse.org/bugs/show_bug.cgi?id=467072</a>
	 */
	@SuppressWarnings("serial")
	public static class EclipseLinkHsqlPlatform extends HSQLPlatform {

		/*
		 * (non-Javadoc)
		 * @see org.eclipse.persistence.internal.databaseaccess.DatabasePlatform#getProcedureCallHeader()
		 */
		@Override
		public String getProcedureCallHeader() {
			return "CALL ";
		}
	}
}
