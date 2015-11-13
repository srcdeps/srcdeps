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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;

public class MojoExecutor {

    private final boolean isWindows;
    private final Logger logger;

    private final MavenExecutor mavenExecutor;
    private final ReleaseEnvironment releaseEnvironment;

    public MojoExecutor(MavenSession session, Logger logger,
            SrcdepsConfiguration configuration) {
        super();
        this.logger = logger;

        final String os = System.getProperty("os.name").toLowerCase();
        this.isWindows = os.startsWith("windows");
        try {
            if (isWindows) {
                /*
                 * this one does not support -q so let's use it only on Windows
                 * where ForkedMavenExecutor does not work
                 */
                this.mavenExecutor = (MavenExecutor) session.lookup(MavenExecutor.ROLE, "invoker");
            } else {
                this.mavenExecutor = (MavenExecutor) session.lookup(MavenExecutor.ROLE, "forked-path");
            }
            logger.debug("srcdeps-maven-plugin looked up a mavenExecutor [" + mavenExecutor.getClass() + "]");
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        }
        this.releaseEnvironment = new DefaultReleaseEnvironment().setJavaHome(configuration.getJavaHome())
                .setMavenHome(configuration.getMavenHome()).setSettings(session.getSettings());
    }

    public void execute(File workingDirectory, String goals, String args) throws MavenExecutorException {
        if (isWindows) {
            logger.info("srcdeps-maven-plugin invoking cd " + workingDirectory.getAbsolutePath() + "; mvn " + goals
                    + " " + args);
        }
        mavenExecutor.executeGoals(workingDirectory, goals, releaseEnvironment, false, args, "pom.xml",
                new ReleaseResult());
    }

    public boolean isWindows() {
        return isWindows;
    }

}
