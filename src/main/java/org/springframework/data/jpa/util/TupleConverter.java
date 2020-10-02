package org.springframework.data.jpa.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class TupleConverter implements Converter<Object, Object> {

    private final Class<?> type;

    /**
     * Creates a new {@link TupleConverter} for the given {@link Class}.
     *
     * @param type must not be {@literal null}.
     */
    public TupleConverter(Class<?> type) {

        Assert.notNull(type, "Returned type must not be null!");

        this.type = type;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
     */
    @Override
    public Object convert(Object source) {

        if (!(source instanceof Tuple)) {
            return source;
        }

        Tuple tuple = (Tuple) source;
        List<TupleElement<?>> elements = tuple.getElements();

        if (elements.size() == 1) {

            Object value = tuple.get(elements.get(0));

            if (type.isInstance(value) || value == null) {
                return value;
            }
        }

        return new TupleBackedMap(tuple);
    }

    /**
     * A {@link Map} implementation which delegates all calls to a {@link Tuple}. Depending on the provided
     * {@link Tuple} implementation it might return the same value for various keys of which only one will appear in the
     * key/entry set.
     *
     * @author Jens Schauder
     */
    private static class TupleBackedMap implements Map<String, Object> {

        private static final String UNMODIFIABLE_MESSAGE = "A TupleBackedMap cannot be modified.";

        private final Tuple tuple;

        TupleBackedMap(Tuple tuple) {
            this.tuple = tuple;
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
         * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code false}.
         * Otherwise this returns {@code true} even when the value from the backing {@code Tuple} is {@code null}.
         *
         * @param key the key for which to get the value from the map.
         * @return wether the key is an element of the backing tuple.
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
         * If the key is not a {@code String} or not a key of the backing {@link Tuple} this returns {@code null}.
         * Otherwise the value from the backing {@code Tuple} is returned, which also might be {@code null}.
         *
         * @param key the key for which to get the value from the map.
         * @return the value of the backing {@link Tuple} for that key or {@code null}.
         */
        @Override
        @Nullable
        public Object get(Object key) {

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
    }

}
