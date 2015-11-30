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
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.config.Repository;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.scm.Scm;
import org.l2x6.maven.srcdeps.scm.ScmException;
import org.l2x6.maven.srcdeps.util.ArgsBuilder;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

public class SrcdepsInstaller {

    private final ArtifactHandlerManager artifactHandlerManager;

    private final SrcdepsConfiguration configuration;
    private final PropsEvaluator evaluator;
    private final Logger logger;
    private final MojoExecutor mojoExecutor;
    private final Map<Dependency, ScmVersion> revisions;
    private final MavenSession session;

    public SrcdepsInstaller(MavenSession session, PropsEvaluator evaluator, Logger logger,
            ArtifactHandlerManager artifactHandlerManager, SrcdepsConfiguration configuration,
            Map<Dependency, ScmVersion> revisions) {
        super();
        this.session = session;
        this.evaluator = evaluator;
        this.logger = logger;
        this.artifactHandlerManager = artifactHandlerManager;
        this.configuration = configuration;
        this.revisions = revisions;
        this.mojoExecutor = new MojoExecutor(session, logger, configuration);
    }

    private void build(DependencyBuild depBuild) throws MavenExecutorException {
        logger.info(
                "srcdeps-maven-plugin is setting [" + depBuild.getId() + "] version [" + depBuild.getVersion() + "]");

        final String versionsArgs = new ArgsBuilder(configuration, session, evaluator, logger)
                .property("newVersion", depBuild.getVersion()).property("generateBackupPoms", "false").build();
        mojoExecutor.execute(depBuild.getWorkingDirectory(), "versions:set", versionsArgs);

        logger.info(
                "srcdeps-maven-plugin is building [" + depBuild.getId() + "] version [" + depBuild.getVersion() + "]");
        final String buildArgs = new ArgsBuilder(configuration, session, evaluator, logger)
                .buildArgs(depBuild.getBuildArgs()) //
                .buildOptions() //
                .build();

        mojoExecutor.execute(depBuild.getWorkingDirectory(), depBuild.getGoalsString(), buildArgs);
    }

    protected Repository findRepository(Dependency dep) {
        for (Repository repository : configuration.getRepositories()) {
            for (String selector : repository.getSelectors()) {
                if (dep.getGroupId().equals(selector)) {
                    return repository;
                }
            }
        }
        return null;
    }

    public void install() throws MavenExecutionException {
        logger.debug("srcdeps-maven-plugin using configuration " + configuration);
        Map<String, DependencyBuild> depBuilds = new HashMap<String, DependencyBuild>(revisions.size());

        ArtifactRepository localRepo = session.getLocalRepository();

        for (Map.Entry<Dependency, ScmVersion> revisionEntry : revisions.entrySet()) {
            Dependency dep = revisionEntry.getKey();

            Artifact artifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                    dep.getScope(), dep.getType(), dep.getClassifier(),
                    artifactHandlerManager.getArtifactHandler(dep.getType()));

            File artifactFile = new File(localRepo.getBasedir(), localRepo.pathOf(artifact));
            if (artifactFile.exists()) {
                logger.info("srcdeps-maven-plugin dependency available in local repository: "
                        + artifactFile.getAbsolutePath());
            } else {
                logger.info("srcdeps-maven-plugin dependency missing in local repository: "
                        + artifactFile.getAbsolutePath());
                ScmVersion scmVersion = revisionEntry.getValue();
                Repository repo = findRepository(dep);
                if (repo == null) {
                    if (configuration.isFailOnMissingRepository()) {
                        throw new RuntimeException("Could not find repository for " + dep);
                    } else {
                        /*
                         * ignore and assume that the user knows what he is
                         * doing
                         */
                        logger.warn("srcdeps-maven-plugin has not found a SCM repository for dependency [" + dep + "]");
                    }
                } else {
                    String url = repo.getDefaultUrl();
                    DependencyBuild depBuild = depBuilds.get(url);
                    if (depBuild != null) {
                        ScmVersion foundScmVersion = depBuild.getScmVersion();
                        if (foundScmVersion != null && !foundScmVersion.equals(scmVersion)) {
                            throw new RuntimeException("Cannot handle two revisions for the same repository URL '" + url
                                    + "': '" + foundScmVersion + "' and '" + scmVersion + "' of " + dep);
                        }
                    } else {
                        /* depBuild == null */
                        depBuild = new DependencyBuild(configuration.getSourcesDirectory(), repo.getId(),
                                repo.getUrls(), dep.getVersion(), scmVersion, repo.isSkipTests(),
                                repo.isMavenTestSkip(), repo.getGoals(), repo.getBuildArgs());
                        depBuilds.put(url, depBuild);
                    }
                }
            }

        }

        /* this could eventually be done in parallel */
        for (DependencyBuild depBuild : depBuilds.values()) {
            try {
                final Scm scm = Scm.forUrl(depBuild.getUrls().iterator().next(), configuration, mojoExecutor, session,
                        logger);
                scm.checkout(depBuild, evaluator);
                build(depBuild);
            } catch (ScmException e) {
                throw new RuntimeException(e);
            } catch (MavenExecutorException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
