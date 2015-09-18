/**
 * Copyright 2015 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.l2x6.maven.srcdeps.util;

public class Optional<T> {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Optional<?> EMPTY = new Optional(null);

    @SuppressWarnings("unchecked")
    public static final <T> Optional<T> empty() {
        return (Optional<T>) EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> ofNullable(T value) {
        return value == null ? (Optional<T>) empty() : new Optional<T>(value);
    }

    private final T value;

    public Optional(T value) {
        super();
        this.value = value;
    }

    public boolean isPresent() {
        return value != null;
    }

    @SuppressWarnings("unchecked")
    public <U> Optional<U> map(Mapper<? super T, ? extends U> mapper) {
        if (!isPresent())
            return empty();
        else {
            return (Optional<U>) ofNullable(mapper.map(value));
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<T> orElseGet(Supplier<? extends T> other) {
        if (!isPresent())
            return (Optional<T>) ofNullable(other.get());
        else {
            return this;
        }
    }

    public T value() {
        return value;
    }

}