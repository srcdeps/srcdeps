/**
 * Copyright 2015-2016 Maven Source Dependencies
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

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import edu.emory.mathcs.backport.java.util.Arrays;

public interface Mapper<T, R> {
    Mapper<Plugin, Xpp3Dom> TO_DOM = new Mapper<Plugin, Xpp3Dom>() {
        @Override
        public Xpp3Dom map(Plugin plugin) {
            Object conf = plugin.getConfiguration();
            if (conf instanceof Xpp3Dom) {
                return (Xpp3Dom) conf;
            }
            return null;
        }
    };

    Mapper<Xpp3Dom, String> NODE_VALUE = new Mapper<Xpp3Dom, String>() {
        @Override
        public String map(Xpp3Dom node) {
            return node != null ? node.getValue() : null;
        }
    };
    Mapper<String, List<String>> TO_STRING_LIST = new Mapper<String, List<String>>() {
        @SuppressWarnings("unchecked")
        @Override
        public List<String> map(String value) {
            return value == null ? null : Collections.unmodifiableList(Arrays.asList(value.split(" ,\t\n\r")));
        }
    };
    Mapper<String, Set<String>> TO_STRING_SET = new Mapper<String, Set<String>>() {
        @SuppressWarnings("unchecked")
        @Override
        public Set<String> map(String value) {
            return value == null ? null
                    : Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(value.split(" ,\t\n\r"))));
        }
    };
    Mapper<String, Boolean> TO_BOOLEAN = new Mapper<String, Boolean>() {
        @Override
        public Boolean map(String value) {
            return value == null ? null : Boolean.valueOf(value);
        }
    };
    Mapper<String, File> TO_FILE = new Mapper<String, File>() {
        @Override
        public File map(String value) {
            return new File(value);
        }
    };

    R map(T t);
}