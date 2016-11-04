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

import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

/**
 * An enhancement of {@link PropertyUtils} that can handle builders following the builder pattern.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuilderPropertyUtils extends PropertyUtils {

    private final Map<Class<?>, Map<String, Property>> propertyMaps;

    public BuilderPropertyUtils(Class<?>... builders) {
        super();
        Map<Class<?>, Map<String, Property>> m = new HashMap<>();
        for (Class<?> builder : builders) {
            m.put(builder, buildMap(builder));
        }
        this.propertyMaps = Collections.unmodifiableMap(m);
    }

    private static Map<String, Property> buildMap(Class<?> builderClass) {
        HashMap<String, Property> m = new HashMap<>();
        for (Method builderMethod : builderClass.getDeclaredMethods()) {
            if (builderMethod.getReturnType() == builderClass && builderMethod.getParameterTypes().length == 1) {
                Property old = m.put(builderMethod.getName(), new BuilderProperty(builderMethod));
                if (old != null) {
                    throw new IllegalStateException(String.format("Cannot handle overloads of method [%s].", builderMethod.getName()));
                }
            }
        }
        return Collections.unmodifiableMap(m);
    }

    @Override
    protected Map<String, Property> getPropertiesMap(Class<?> type, BeanAccess bAccess)
            throws IntrospectionException {
        Map<String, Property> result = propertyMaps.get(type);
        return result != null ? result : super.getPropertiesMap(type, bAccess);
    }

}