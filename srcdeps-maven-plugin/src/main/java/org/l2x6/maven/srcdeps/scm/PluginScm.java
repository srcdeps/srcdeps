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
package org.l2x6.maven.srcdeps.scm;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.DependencyBuild;
import org.l2x6.maven.srcdeps.MojoExecutor;
import org.l2x6.maven.srcdeps.ScmVersion;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.util.ArgsBuilder;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

public class PluginScm extends Scm {

    public PluginScm(SrcdepsConfiguration configuration, MojoExecutor mojoExecutor, MavenSession session,
            Logger logger) {
        super(configuration, mojoExecutor, session, logger);
    }

    @Override
    protected void checkout(String url, File checkoutDir, DependencyBuild depBuild, PropsEvaluator evaluator)
            throws ScmException {
        try {
            final ScmVersion scmVersion = depBuild.getScmVersion();

            final String args = new ArgsBuilder(configuration, session, evaluator, logger)
                    .property("checkoutDirectory", checkoutDir.getAbsolutePath()).property("connectionUrl", url)
                    .property("scmVersionType", scmVersion.getVersionType())
                    .property("scmVersion", scmVersion.getVersion())
                    .nonDefaultProp("skipTests", depBuild.isSkipTests(), false)
                    .nonDefaultProp("maven.test.skip", depBuild.isMavenTestSkip(), false).build();

            File workingDirectory = new File(session.getExecutionRootDirectory());
            String goals = "org.apache.maven.plugins:maven-scm-plugin:" + configuration.getScmPluginVersion()
                    + ":checkout";
            if (mojoExecutor.isWindows()) {
                logger.info("srcdeps-maven-plugin invoking cd " + workingDirectory.getAbsolutePath() + "; mvn "
                        + goals + " " + args);
            }
            mojoExecutor.execute(workingDirectory, goals, args);
        } catch (MavenExecutorException e) {
            throw new ScmException(e);
        }

    }
}
