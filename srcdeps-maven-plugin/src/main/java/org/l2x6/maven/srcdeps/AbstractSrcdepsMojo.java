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
import java.util.List;

import org.apache.maven.execution.MavenSession;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.l2x6.maven.srcdeps.config.Repository;

/**
 * Goal which touches a timestamp file.
 *
 */
public abstract class AbstractSrcdepsMojo extends AbstractMojo {

    private static final Object modelLock = new Object();

    private static final String REVISIONS_MAP_KEY = "srcdeps.revisions";

    @Parameter(property = "srcdeps.failOnMissingRepository", defaultValue = "true", required = true)
    protected boolean failOnMissingRepository;

    @Parameter(property = "srcdeps.javaHome", defaultValue = "${java.home}", required = true)
    protected File javaHome;

    @Parameter(property = "srcdeps.mavenHome", defaultValue = "${maven.home}", required = true)
    protected File mavenHome;

    @Component
    protected BuildPluginManager pluginManager;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(required = true)
    protected List<Repository> repositories;

    @Parameter(property = "srcdeps.scmPluginVersion", defaultValue = "1.9.4", required = true)
    protected String scmPluginVersion;

    /**
     * The session
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

}
