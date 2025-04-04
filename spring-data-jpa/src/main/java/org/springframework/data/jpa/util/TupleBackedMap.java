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
package org.springframework.data.jpa.util;

import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.support.JdbcUtils;

/**
 * A {@link Map} implementation which delegates all calls to a {@link Tuple}. Depending on the provided {@link Tuple}
 * implementation it might return the same value for various keys of which only one will appear in the key/entry set.
 *
 * @author Jens Schauder
 * @since 4.0
 */
public class TupleBackedMap implements Map<String, Object> {

	private static final String UNMODIFIABLE_MESSAGE = "A TupleBackedMap cannot be modified";

	private final Tuple tuple;

	public TupleBackedMap(Tuple tuple) {
		this.tuple = tuple;
	}

	/**
	 * Creates a underscore-aware {@link Tuple} wrapper applying {@link JdbcUtils#convertPropertyNameToUnderscoreName}
	 * conversion to leniently look up properties from query results whose columns follow snake-case syntax.
	 *
	 * @param delegate the tuple to wrap.
	 * @return
	 */
	public static Tuple underscoreAware(Tuple delegate) {
		return new FallbackTupleWrapper(delegate);
	}

	@Override
	public int size() {
		return tuple.getElements().size();
	}

	@Override
	public boolean isEmpty() {
		return tuple.getElements().isEmpty();
	}

	/**
	 * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code false}. Otherwise
	 * this returns {@code true} even when the value from the backing {@code Tuple} is {@code null}.
	 *
	 * @param key the key for which to get the value from the map.
	 * @return whether the key is an element of the backing tuple.
	 */
	@Override
	public boolean containsKey(Object key) {

		try {
			tuple.get((String) key);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@Override
	public boolean containsValue(Object value) {
		return Arrays.asList(tuple.toArray()).contains(value);
	}

	/**
	 * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code null}. Otherwise
	 * the value from the backing {@code Tuple} is returned, which also might be {@code null}.
	 *
	 * @param key the key for which to get the value from the map.
	 * @return the value of the backing {@link Tuple} for that key or {@code null}.
	 */
	@Override
	public @Nullable Object get(Object key) {

		if (!(key instanceof String)) {
			return null;
		}

		try {
			return tuple.get((String) key);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
	}

	@Override
	public void putAll(Map<? extends String, ?> m) {
		throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(UNMODIFIABLE_MESSAGE);
	}

	@Override
	public Set<String> keySet() {

		return tuple.getElements().stream() //
				.map(TupleElement::getAlias) //
				.collect(Collectors.toSet());
	}

	@Override
	public Collection<Object> values() {
		return Arrays.asList(tuple.toArray());
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {

		return tuple.getElements().stream() //
				.map(e -> new HashMap.SimpleEntry<String, Object>(e.getAlias(), tuple.get(e))) //
				.collect(Collectors.toSet());
	}

	static class FallbackTupleWrapper implements Tuple {

		private final Tuple delegate;
		private final UnaryOperator<String> fallbackNameTransformer = JdbcUtils::convertPropertyNameToUnderscoreName;

		FallbackTupleWrapper(Tuple delegate) {
			this.delegate = delegate;
		}

		@Override
		public <X> X get(TupleElement<X> tupleElement) {
			return get(tupleElement.getAlias(), tupleElement.getJavaType());
		}

		@Override
		public <X> X get(String s, Class<X> type) {
			try {
				return delegate.get(s, type);
			} catch (IllegalArgumentException original) {
				try {
					return delegate.get(fallbackNameTransformer.apply(s), type);
				} catch (IllegalArgumentException next) {
					original.addSuppressed(next);
					throw original;
				}
			}
		}

		@Override
		public Object get(String s) {
			try {
				return delegate.get(s);
			} catch (IllegalArgumentException original) {
				try {
					return delegate.get(fallbackNameTransformer.apply(s));
				} catch (IllegalArgumentException next) {
					original.addSuppressed(next);
					throw original;
				}
			}
		}

		@Override
		public <X> X get(int i, Class<X> aClass) {
			return delegate.get(i, aClass);
		}

		@Override
		public Object get(int i) {
			return delegate.get(i);
		}

		@Override
		public Object[] toArray() {
			return delegate.toArray();
		}

		@Override
		public List<TupleElement<?>> getElements() {
			return delegate.getElements();
		}
	}
}
