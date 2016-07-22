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
package org.l2x6.srcdeps.core;

import java.util.Objects;

/**
 * A pair of {@link #scmVersion} and {@link #scmVersionType}, the two having the same semantics as {@code <scmVersion>}
 * and {@code <scmVersionType>} in <a href="https://maven.apache.org/scm/maven-scm-plugin/checkout-mojo.html">
 * maven-scm-plugin</a>.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcVersion {
    /**
     * Some well known version types
     */
    public enum WellKnownType {
        branch, revision, tag
    }

    public static final char SRC_VERSION_DELIMITER = '-';
    public static final String SRC_VERSION_INFIX = "-SRC-";

    public static boolean isSrcVersion(String rawVersion) {
        return rawVersion.indexOf(SRC_VERSION_INFIX) >= 0;
    }

    /**
     * @param rawVersion
     *            the string to parse
     * @return a new instance of {@link SrcVersion} as parsed from the given {@code rawString}
     */
    public static SrcVersion parse(String rawVersion) {
        int pos = rawVersion.indexOf(SRC_VERSION_INFIX);
        if (pos >= 0) {
            int versionTypeStart = pos + SRC_VERSION_INFIX.length();
            int versionTypeEnd = rawVersion.indexOf(SRC_VERSION_DELIMITER, versionTypeStart);
            if (versionTypeEnd >= 0) {
                String versionType = rawVersion.substring(versionTypeStart, versionTypeEnd);
                return new SrcVersion(rawVersion, versionType, rawVersion.substring(versionTypeEnd + 1));
            } else {
                throw new RuntimeException("Version string '" + rawVersion + "' contains '" + SRC_VERSION_INFIX
                        + " that is not followed by a version type such as 'tag', 'branch', or 'revision'.");
            }
        } else {
            return null;
        }
    };

    /** What we parsed from, this is also returned by {@link #toString()} */
    private final String rawString;

    /**
     * See https://maven.apache.org/scm/maven-scm-plugin/checkout-mojo.html# scmVersion
     */
    private final String scmVersion;

    /**
     * See https://maven.apache.org/scm/maven-scm-plugin/checkout-mojo.html# scmVersionType
     */
    private final String scmVersionType;

    private SrcVersion(String rawString, String scmVersionType, String scmVersion) {
        super();
        this.rawString = rawString;
        this.scmVersionType = scmVersionType;
        this.scmVersion = scmVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SrcVersion) {
            SrcVersion other = (SrcVersion) o;
            return Objects.equals(this.rawString, other.rawString);
        }
        return false;
    }

    /**
     * @return version see {@link #scmVersionType}
     */
    public String getScmVersion() {
        return scmVersion;
    }

    /**
     * @return versionType see {@link #scmVersion}
     */
    public String getScmVersionType() {
        return scmVersionType;
    }

    /**
     * @return {@code WellKnownType.valueOf(scmVersionType)}
     */
    public WellKnownType getWellKnownType() {
        return WellKnownType.valueOf(scmVersionType);
    }

    @Override
    public int hashCode() {
        return rawString.hashCode();
    }

    @Override
    public String toString() {
        return rawString;
    }

}
