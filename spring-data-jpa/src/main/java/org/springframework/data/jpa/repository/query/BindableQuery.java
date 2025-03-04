/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import java.util.Collections;
import java.util.List;


/**
 * @author Christoph Strobl
 */
final class BindableQuery implements DeclaredQuery {

    private final DeclaredQuery source;
    private final String bindableQueryString;
    private final List<ParameterBinding> bindings;
    private final boolean usesJdbcStyleParameters;

    public BindableQuery(DeclaredQuery source, String bindableQueryString, List<ParameterBinding> bindings, boolean usesJdbcStyleParameters) {
        this.source = source;
        this.bindableQueryString = bindableQueryString;
        this.bindings = bindings;
        this.usesJdbcStyleParameters = usesJdbcStyleParameters;
    }

    @Override
    public boolean isNativeQuery() {
        return source.isNativeQuery();
    }

    boolean hasBindings() {
        return !bindings.isEmpty();
    }

    boolean usesJdbcStyleParameters() {
        return usesJdbcStyleParameters;
    }

    @Override
    public String getQueryString() {
        return bindableQueryString;
    }

    public BindableQuery unifyBindings(BindableQuery comparisonQuery) {
        if (comparisonQuery.hasBindings() && !comparisonQuery.bindings.equals(this.bindings)) {
            return new BindableQuery(source, bindableQueryString, comparisonQuery.bindings, usesJdbcStyleParameters);
        }
        return this;
    }

    public List<ParameterBinding> getBindings() {
        return Collections.unmodifiableList(bindings);
    }
}
