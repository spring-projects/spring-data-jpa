/*
 * Copyright 2025-present the original author or authors.
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

import java.util.Optional;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.jspecify.annotations.Nullable;

/**
 * {@code CurrentTenantIdentifierResolver} instance for testing.
 *
 * @author Ariel Morelli Andres
 */
public class HibernateCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

	private static final ThreadLocal<@Nullable String> CURRENT_TENANT_IDENTIFIER = new ThreadLocal<>();

	static void setTenantIdentifier(String tenantIdentifier) {
		CURRENT_TENANT_IDENTIFIER.set(tenantIdentifier);
	}

	static void removeTenantIdentifier() {
		CURRENT_TENANT_IDENTIFIER.remove();
	}

	@Override
	public String resolveCurrentTenantIdentifier() {
		return Optional.ofNullable(CURRENT_TENANT_IDENTIFIER.get())
				.orElseThrow(() -> new IllegalArgumentException("Could not resolve current tenant identifier"));
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

}
