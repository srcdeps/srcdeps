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

import java.io.File;
import java.util.Collection;
import java.util.List;

public class DependencyBuild {
    private final String id;
    private final boolean mavenTestSkip;
    private final ScmVersion scmVersion;
    private final boolean skipTests;
    private final Collection<String> urls;
    private final String version;
    private final File workingDirectory;
    private final List<String> goals;
    private final List<String> buildArgs;

    public DependencyBuild(File sourcesDirectory, String id, Collection<String> urls, String version,
            ScmVersion scmVersion, boolean skipTests, boolean mavenTestSkip, List<String> goals,
            List<String> buildArgs) {
        super();
        this.id = id;
        this.urls = urls;
        this.scmVersion = scmVersion;
        this.version = version;
        this.skipTests = skipTests;
        this.mavenTestSkip = mavenTestSkip;
        this.goals = goals;
        this.buildArgs = buildArgs;

        this.workingDirectory = new File(sourcesDirectory, id);
    }

    public String getId() {
        return id;
    }

    public ScmVersion getScmVersion() {
        return scmVersion;
    }

    public Collection<String> getUrls() {
        return urls;
    }

    public String getVersion() {
        return version;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public boolean isMavenTestSkip() {
        return mavenTestSkip;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public List<String> getGoals() {
        return goals;
    }

    public String getGoalsString() {
        StringBuilder sb = new StringBuilder();
        for (String goal : goals) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(goal);
        }
        return sb.toString();
    }

    public List<String> getBuildArgs() {
        return buildArgs;
    }

}
