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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.config.Repository;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;

public class SrcdepsInstaller {

    private final Logger logger;

    private final SrcdepsConfiguration configuration;
    private final Map<Dependency, String> revisions;
    private final MavenSession session;
    private final MavenExecutor mavenExecutor;
    private final ReleaseEnvironment releaseEnvironment;
    private final ArtifactHandlerManager artifactHandlerManager;

    public SrcdepsInstaller(MavenSession session, Logger logger, ArtifactHandlerManager artifactHandlerManager,
            SrcdepsConfiguration configuration, Map<Dependency, String> revisions) {
        super();
        this.session = session;
        this.logger = logger;
        this.artifactHandlerManager = artifactHandlerManager;
        this.configuration = configuration;
        this.revisions = revisions;
        try {
            this.mavenExecutor = (MavenExecutor) session.lookup(MavenExecutor.ROLE, "forked-path");
            logger.info("mavenExecutor = "+ mavenExecutor);
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        }
        this.releaseEnvironment = new DefaultReleaseEnvironment().setJavaHome(configuration.getJavaHome())
                .setMavenHome(configuration.getMavenHome()).setSettings(session.getSettings());

    }

    protected void build(DependencyBuild depBuild) throws MavenExecutorException {
        mavenExecutor.executeGoals(depBuild.getWorkingDirectory(), "versions:set", releaseEnvironment, false,
                "-D newVersion=" + depBuild.getVersion() + " -DgenerateBackupPoms=false", "pom.xml",
                new ReleaseResult());

        mavenExecutor.executeGoals(depBuild.getWorkingDirectory(), "clean install", releaseEnvironment, false, null,
                "pom.xml", new ReleaseResult());
    }

    protected void checkout(DependencyBuild depBuild) throws MavenExecutorException {
        File checkoutDir = depBuild.getWorkingDirectory();
        if (!checkoutDir.exists()) {
            checkoutDir.mkdirs();
        }

        String srcAbsPath = checkoutDir.getAbsolutePath();

        String args = "-DcheckoutDirectory=" + srcAbsPath + " -DconnectionUrl=" + depBuild.getUrl()
                + " -DscmVersionType=revision -DscmVersion=" + depBuild.getRevisionId();

        mavenExecutor.executeGoals(new File(session.getExecutionRootDirectory()),
                "org.apache.maven.plugins:maven-scm-plugin:" + configuration.getScmPluginVersion() + ":checkout",
                releaseEnvironment, false, args, "pom.xml", new ReleaseResult());

    }

    protected Repository findRepository(Dependency dep) {
        for (Repository repository : configuration.getRepositories()) {
            for (String selector : repository.getSelectors()) {
                if (SrcdepsUtils.matches(dep, selector)) {
                    return repository;
                }
            }
        }
        return null;
    }

    public void install() {
        logger.info("About to build srcdeps with " + configuration);
        Map<String, DependencyBuild> depBuilds = new HashMap<String, DependencyBuild>(revisions.size());

        ArtifactRepository localRepo = session.getLocalRepository();

        for (Map.Entry<Dependency, String> revisionEntry : revisions.entrySet()) {
            Dependency dep = revisionEntry.getKey();

            Artifact artifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                    dep.getScope(), dep.getType(), dep.getClassifier(),
                    artifactHandlerManager.getArtifactHandler(dep.getType()));

            File artifactFile = new File(localRepo.getBasedir(), localRepo.pathOf(artifact));
            if (artifactFile.exists()) {
                logger.info("Source dependency available in local repository: "+ artifactFile.getAbsolutePath());
            } else {
                logger.info("Source dependency missing in local repository: "+ artifactFile.getAbsolutePath());
                String depRevisionId = revisionEntry.getValue();
                Repository repo = findRepository(dep);
                if (repo == null) {
                    if (configuration.isFailOnMissingRepository()) {
                        throw new RuntimeException("Could not find repository for " + dep);
                    } else {
                        /* ignore and assume that the user knows what he is doing */
                    }
                } else {
                    String url = repo.getUrl();
                    DependencyBuild depBuild = depBuilds.get(url);
                    if (depBuild != null) {
                        String foundRevisionId = depBuild.getRevisionId();
                        if (foundRevisionId != null && !foundRevisionId.equals(depRevisionId)) {
                            throw new RuntimeException("Cannot handle two revisions for the same repository URL '" + url
                                    + "': '" + foundRevisionId + "' and '" + depRevisionId + "' of " + dep);
                        }
                    } else {
                        /* checkout == null */
                        depBuild = new DependencyBuild(configuration.getSourcesDirectory(), repo.getId(), url,
                                dep.getVersion(), depRevisionId);
                        depBuilds.put(url, depBuild);
                    }
                }
            }

        }

        /* this could eventually be done in parallel */
        for (DependencyBuild depBuild : depBuilds.values()) {
            try {
                checkout(depBuild);
                build(depBuild);
            } catch (MavenExecutorException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
