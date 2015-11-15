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

public class ScmVersion {

    public enum WellKnownType { branch, tag, revision };

    public static ScmVersion fromSrcdepsVersionString(String version) {
        int pos = version.indexOf(SrcdepsConstants.SRC_VERSION_INFIX);
        if (pos >= 0) {
            int versionTypeStart = pos + SrcdepsConstants.SRC_VERSION_INFIX.length();
            int versionTypeEnd = version.indexOf(SrcdepsConstants.SRC_VERSION_DELIMITER, versionTypeStart);
            if (versionTypeEnd >= 0) {
                String versionType = version.substring(versionTypeStart, versionTypeEnd);
                return new ScmVersion(versionType, version.substring(versionTypeEnd + 1));
            } else {
                throw new RuntimeException(
                        "Version string '" + version + "' contains '" + SrcdepsConstants.SRC_VERSION_INFIX
                                + " that is not followed by a version type such as 'tag', 'branch', or 'revision'.");
            }
        } else {
            return null;
        }
    }

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

    public ScmVersion(String versionType, String version) {
        super();
        this.versionType = versionType;
        this.version = version;
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

    public WellKnownType getWellKnownType() {
        return WellKnownType.valueOf(versionType);
    }

}
