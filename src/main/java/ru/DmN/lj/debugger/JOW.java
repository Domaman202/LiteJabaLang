package ru.DmN.lj.debugger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Java Object Wrapper
 */
public class JOW {
    public static <T> WrappedObject<T> wrap(T o) {
        return new WrappedObject<>(o);
    }

    public static class WrappedObject <T> implements Map<String, Object> {
        public final T wrapped;
        public final Class<T> clazz;

        public WrappedObject(T wrapped) {
            this.wrapped = wrapped;
            this.clazz = (Class<T>) wrapped.getClass();
        }

        @Override
        public int size() {
            return this.clazz.getDeclaredFields().length;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return Arrays.stream(this.clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(key));
        }

        @Override
        public boolean containsValue(Object value) {
                return false; // NOT SUPPORTED
        }

        @Override
        public Object get(Object key) {
            try {
                return this.clazz.getDeclaredField((String) key).get(this.wrapped);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object put(String key, Object value) {
            try {
                var f = this.clazz.getDeclaredField(key);
                var old = f.get(this.wrapped);
                f.set(this.wrapped, value);
                return old;
            } catch (IllegalAccessException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object remove(Object key) {
            return null; // NOT SUPPORTED
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
            // TODO:
        }

        @Override
        public void clear() {
            // NOT SUPPORTED
        }

        @Override
        public Set<String> keySet() {
            return null; // TODO:
        }

        @Override
        public Collection<Object> values() {
            return null; // TODO:
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return null; // TODO:
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return this.wrapped.hashCode();
        }

        @Override
        public String toString() {
            return String.valueOf(this.wrapped);
        }
    }
}
