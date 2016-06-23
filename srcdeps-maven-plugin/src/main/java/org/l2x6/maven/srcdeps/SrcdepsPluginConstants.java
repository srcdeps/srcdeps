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
package org.l2x6.maven.srcdeps;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public interface SrcdepsPluginConstants {
    String ORG_L2X6_MAVEN_SRCDEPS_GROUP_ID = "org.l2x6.maven.srcdeps";
    String SRCDEPS_MAVEN_PLUGIN_ADRTIFACT_ID = "srcdeps-maven-plugin";
    Set<String> DEFAULT_FAIL_WITH_PROFILES = Collections
            .unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("release")));

}
