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
package org.srcdeps.config.yaml.internal;

import java.lang.reflect.Method;

import org.yaml.snakeyaml.introspector.GenericProperty;
import org.yaml.snakeyaml.introspector.Property;

/**
 * A {@link Property} suitable for builders following the fluent builder pattern.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuilderProperty extends GenericProperty {

    private final Method builderMethod;

    public BuilderProperty(Method builderMethod) {
        super(builderMethod.getName(), builderMethod.getParameterTypes()[0],
                builderMethod.getGenericParameterTypes()[0]);
        this.builderMethod = builderMethod;
    }

    @Override
    public Object get(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(Object object, Object value) throws Exception {
        builderMethod.invoke(object, value);
    }

}