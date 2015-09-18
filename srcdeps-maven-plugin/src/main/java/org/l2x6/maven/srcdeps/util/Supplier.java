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

public interface Supplier<T> {
    public static class Constant<T> implements Supplier<T> {
        public static final Supplier<String> EMPTY_STRING = new Supplier.Constant<String>("");
        public static final Supplier<Boolean> FALSE = new Supplier.Constant<Boolean>(Boolean.FALSE);
        private final T value;

        public Constant(T value) {
            super();
            this.value = value;
        }

        public T get() {
            return value;
        }
    }

    T get();
}