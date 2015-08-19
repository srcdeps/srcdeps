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
package org.l2x6.maven.srcdeps;

import org.apache.maven.model.Dependency;

public class SrcdepsUtils {
    public static String getSourceRevision(String version) {
        int pos = version.indexOf(SrcdepsConstants.SRC_VERSION_INFIX);
        if (pos >= 0) {
            return version.substring(pos + SrcdepsConstants.SRC_VERSION_INFIX.length());
        } else {
            return null;
        }
    }
    public static boolean matches(Dependency dependency, String selector) {
        /* as simple as possible initially */
        return dependency.getGroupId().equals(selector);
    }
}
