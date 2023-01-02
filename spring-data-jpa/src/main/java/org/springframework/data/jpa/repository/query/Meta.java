/*
 * Copyright 2014-2023 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Value object to hold metadata about repository methods.
 *
 * @author Greg Turnquist
 * @since 3.0
 * @see org.springframework.data.jpa.repository.Meta
 */
public class Meta {

	private enum MetaKey {
		COMMENT("comment");

		private String key;

		MetaKey(String key) {
			this.key = key;
		}
	}

	private Map<String, Object> values = Collections.emptyMap();

	public Meta() {}

	/**
	 * Copy a {@link Meta} object.
	 *
	 * @since 3.0
	 * @param source
	 */
	Meta(Meta source) {
		this.values = new LinkedHashMap<>(source.values);
	}

	/**
	 * Add a comment to the query that is propagated to the profile log.
	 *
	 * @param comment
	 */
	public void setComment(String comment) {
		setValue(MetaKey.COMMENT.key, comment);
	}

	/**
	 * @return {@literal null} if not set.
	 */
	@Nullable
	public String getComment() {
		return getValue(MetaKey.COMMENT.key);
	}

	/**
	 * @return
	 */
	public boolean hasValues() {
		return !this.values.isEmpty();
	}

	/**
	 * Get {@link Iterable} of set meta values.
	 *
	 * @return
	 */
	public Iterable<Map.Entry<String, Object>> values() {
		return Collections.unmodifiableSet(this.values.entrySet());
	}

	/**
	 * Sets or removes the value in case of {@literal null} or empty {@link String}.
	 *
	 * @param key must not be {@literal null} or empty.
	 * @param value
	 */
	void setValue(String key, @Nullable Object value) {

		Assert.hasText(key, "Meta key must not be 'null' or blank");

		if (values == Collections.EMPTY_MAP) {
			values = new LinkedHashMap<>(2);
		}

		if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
			this.values.remove(key);
		}
		this.values.put(key, value);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private <T> T getValue(String key) {
		return (T) this.values.get(key);
	}

}
