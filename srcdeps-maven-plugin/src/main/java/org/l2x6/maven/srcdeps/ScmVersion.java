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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class ScmVersion {

    public static final class ScmVersionElement {

        /**
         * See https://maven.apache.org/scm/maven-scm-plugin/checkout-mojo.html#
         * scmVersion
         */
        private final String version;

        /**
         * See https://maven.apache.org/scm/maven-scm-plugin/checkout-mojo.html#
         * scmVersionType
         */
        private final String versionType;

        public ScmVersionElement(String versionType, String version) {
            super();
            this.version = version;
            this.versionType = versionType;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ScmVersionElement other = (ScmVersionElement) obj;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            if (versionType == null) {
                if (other.versionType != null)
                    return false;
            } else if (!versionType.equals(other.versionType))
                return false;
            return true;
        }

        /**
         * @return version see {@link #versionType}
         */
        public String getVersion() {
            return version;
        }

        /**
         * @return versionType see {@link #version}
         */
        public String getVersionType() {
            return versionType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            result = prime * result + ((versionType == null) ? 0 : versionType.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "ScmVersionElement [version=" + version + ", versionType=" + versionType + "]";
        }
    }

    public static ScmVersion fromSrcdepsVersionString(String version) {
        int pos = version.indexOf(SrcdepsConstants.SRC_VERSION_INFIX);
        if (pos >= 0) {
            List<ScmVersionElement> elements = new ArrayList<ScmVersion.ScmVersionElement>();
            int versionTypeStart = pos + SrcdepsConstants.SRC_VERSION_INFIX.length();

            StringTokenizer st = new StringTokenizer(version.substring(versionTypeStart),
                    String.valueOf(SrcdepsConstants.SRC_VERSION_ELEMENT_DELIMITER));
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int versionTypeEnd = token.indexOf(SrcdepsConstants.SRC_VERSION_DELIMITER);
                if (versionTypeEnd >= 0) {
                    String versionType = token.substring(0, versionTypeEnd);
                    elements.add(new ScmVersionElement(versionType, token.substring(versionTypeEnd + 1)));
                } else {
                    throw new RuntimeException("Version string '" + version
                            + "' must contain pairs of scmVersionType-scmVersion separated by semicolon.");
                }

            }

            if (elements.isEmpty()) {
                throw new RuntimeException(
                        "Version string '" + version + "' must contain at last one scmVersionType-scmVersion pair.");
            } else {
                return new ScmVersion(Collections.unmodifiableList(elements));
            }

        } else {
            return null;
        }
    }

    private final List<ScmVersionElement> elements;

    public List<ScmVersionElement> getElements() {
        return elements;
    }

    public ScmVersion(List<ScmVersionElement> elements) {
        super();
        this.elements = elements;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScmVersion other = (ScmVersion) obj;
        if (elements == null) {
            if (other.elements != null)
                return false;
        } else if (!elements.equals(other.elements))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((elements == null) ? 0 : elements.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "ScmVersion [elements=" + elements + "]";
    }

}
