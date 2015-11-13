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
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.l2x6.maven.srcdeps.DependencyBuild;
import org.l2x6.maven.srcdeps.MojoExecutor;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

public abstract class Scm {
    public Scm(SrcdepsConfiguration configuration, MojoExecutor mojoExecutor, MavenSession session, Logger logger) {
        super();
        this.configuration = configuration;
        this.mojoExecutor = mojoExecutor;
        this.session = session;
        this.logger = logger;
    }

    protected final SrcdepsConfiguration configuration;
    protected final MojoExecutor mojoExecutor;
    protected final MavenSession session;
    protected final Logger logger;

    public static Scm forUrl(String url, SrcdepsConfiguration configuration, MojoExecutor mojoExecutor,
            MavenSession session, Logger logger) {
        logger.debug(
                "srcdeps-maven-plugin choosing " + Scm.class.getName() + " implementation based on URL '" + url + "'");
        if (url.startsWith(JGitScm.SCM_GIT_PREFIX)) {
            return new JGitScm(configuration, mojoExecutor, session, logger);
        } else {
            return new PluginScm(configuration, mojoExecutor, session, logger);
        }
    }

    public final void checkout(DependencyBuild depBuild, PropsEvaluator evaluator) throws ScmException {

        Collection<String> urls = depBuild.getUrls();
        int i = 0;
        ScmException executorException = null;
        File checkoutDir = depBuild.getWorkingDirectory();
        if (checkoutDir.exists()) {
            try {
                logger.debug("srcdeps-maven-plugin is about to delete [" + checkoutDir.getAbsolutePath() + "]");
                FileUtils.deleteDirectory(checkoutDir);
            } catch (IOException e) {
                throw new ScmException("srcdeps-maven-plugin could not delete [" + checkoutDir.getAbsolutePath() + "]",
                        e);
            }
        }
        if (checkoutDir.exists()) {
            throw new RuntimeException("srcdeps-maven-plugin could not assert that [" + checkoutDir.getAbsolutePath()
                    + "] does not exist");
        }
        if (!checkoutDir.exists()) {
            checkoutDir.mkdirs();
        }
        for (String url : urls) {
            executorException = null;
            logger.info("srcdeps-maven-plugin [" + getClass().getSimpleName() + "] is checking out [" + depBuild.getId()
                    + "] version [" + depBuild.getVersion() + "] from URL [" + i + "] [" + url + "]");
            i++;

            try {
                checkout(url, checkoutDir, depBuild, evaluator);
                /* break the url loop on first success */
                return;
            } catch (ScmException e) {
                logger.info("srcdeps-maven-plugin could not check out [" + depBuild.getId() + "] version ["
                        + depBuild.getVersion() + "] from URL [" + i + "] [" + url + "] : " + e.getMessage());
                executorException = e;
            }

        }

        if (executorException != null) {
            throw executorException;
        }

    }

    protected abstract void checkout(String url, File checkoutDir, DependencyBuild depBuild, PropsEvaluator evaluator)
            throws ScmException;

}
