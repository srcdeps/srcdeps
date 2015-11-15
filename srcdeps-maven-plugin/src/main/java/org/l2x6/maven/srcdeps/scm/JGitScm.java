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
import org.codehaus.plexus.logging.Logger;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.l2x6.maven.srcdeps.DependencyBuild;
import org.l2x6.maven.srcdeps.MojoExecutor;
import org.l2x6.maven.srcdeps.ScmVersion;
import org.l2x6.maven.srcdeps.ScmVersion.WellKnownType;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

public class JGitScm extends Scm {

    public static final String SCM_GIT_PREFIX = "scm:git:";

    public JGitScm(SrcdepsConfiguration configuration, MojoExecutor mojoExecutor, MavenSession session, Logger logger) {
        super(configuration, mojoExecutor, session, logger);
    }

    @Override
    protected void checkout(String url, File checkoutDir, DependencyBuild depBuild, PropsEvaluator evaluator)
            throws ScmException {

        String useUrl = url.substring(SCM_GIT_PREFIX.length());
        ScmVersion scmVersion = depBuild.getScmVersion();

        CloneCommand cmd = Git.cloneRepository().setURI(useUrl).setDirectory(checkoutDir);

        switch (scmVersion.getWellKnownType()) {
        case branch:
        case tag:
            cmd.setBranch(scmVersion.getVersion());
            break;
        case revision:
            cmd.setCloneAllBranches(true);
            break;
        default:
            throw new IllegalStateException(
                    "Unexpected " + WellKnownType.class.getName() + " value '" + scmVersion.getWellKnownType() + "'.");
        }

        try (Git git = cmd.call()) {
            git.checkout().setName(scmVersion.getVersion()).call();

            // workaround for
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=474093
            git.getRepository().close();
        } catch (Exception e) {
            throw new ScmException(e);
        }

    }

}
