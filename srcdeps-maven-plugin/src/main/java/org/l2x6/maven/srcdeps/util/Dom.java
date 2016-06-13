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

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class Dom {

    private Dom() {
    }

    public static Mapper<Xpp3Dom, Xpp3Dom> getOrCreateChild(final String element) {
        return new Mapper<Xpp3Dom, Xpp3Dom>() {
            @Override
            public Xpp3Dom map(Xpp3Dom parent) {
                Xpp3Dom result = parent.getChild(element);
                if (result == null) {
                    result = new Xpp3Dom(element);
                    parent.addChild(result);
                }
                return result;
            }
        };
    }

    public static Supplier<Xpp3Dom> newElement(final Xpp3Dom parent, final String element) {
        return new Supplier<Xpp3Dom>() {
            @Override
            public Xpp3Dom get() {
                Xpp3Dom result = new Xpp3Dom(element);
                parent.addChild(result);
                return result;
            }
        };
    }

}
