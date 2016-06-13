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

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.config.SrcdepsRepository;
import org.l2x6.srcdeps.core.BuildException;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.BuildService;
import org.l2x6.srcdeps.core.SrcVersion;

public class SrcdepsInstaller {

    private final ArtifactHandlerManager artifactHandlerManager;

    private final SrcdepsConfiguration configuration;
    private final Logger logger;
    private final Map<Dependency, SrcVersion> revisions;
    private final MavenSession session;
    private final BuildService buildService;

    public SrcdepsInstaller(MavenSession session, Logger logger, ArtifactHandlerManager artifactHandlerManager,
            SrcdepsConfiguration configuration, Map<Dependency, SrcVersion> revisions, BuildService buildService) {
        super();
        this.session = session;
        this.logger = logger;
        this.artifactHandlerManager = artifactHandlerManager;
        this.configuration = configuration;
        this.revisions = revisions;
        this.buildService = buildService;
    }

    protected SrcdepsRepository findRepository(Dependency dep) {
        for (SrcdepsRepository repository : configuration.getRepositories()) {
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
        Map<String, BuildRequest> depBuilds = new HashMap<String, BuildRequest>(revisions.size());

        ArtifactRepository localRepo = session.getLocalRepository();

        for (Map.Entry<Dependency, SrcVersion> revisionEntry : revisions.entrySet()) {
            Dependency dep = revisionEntry.getKey();

            Artifact artifact = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getVersion(),
                    dep.getScope(), dep.getType(), dep.getClassifier(),
                    artifactHandlerManager.getArtifactHandler(dep.getType()));

            File artifactFile = new File(localRepo.getBasedir(), localRepo.pathOf(artifact));
            if (artifactFile.exists()) {
                logger.info("srcdeps-maven-plugin dependency available in local repository: "
                        + artifactFile.getAbsolutePath());
            } else {
                logger.info("srcdeps-maven-plugin dependency missing in local repository: ff "
                        + artifactFile.getAbsolutePath());
                SrcVersion srcVersion = revisionEntry.getValue();
                SrcdepsRepository repo = findRepository(dep);
                if (repo == null) {
                    if (configuration.isFailOnMissingRepository()) {
                        throw new RuntimeException("Could not find repository for " + dep);
                    } else {
                        /*
                         * ignore and assume that the user knows what he is doing
                         */
                        logger.warn("srcdeps-maven-plugin has not found a SCM repository for dependency [" + dep + "]");
                    }
                } else {
                    String url = repo.getDefaultUrl();
                    BuildRequest request = depBuilds.get(url);
                    if (request != null) {
                        SrcVersion foundScmVersion = request.getSrcVersion();
                        if (foundScmVersion != null && !foundScmVersion.equals(srcVersion)) {
                            throw new RuntimeException("Cannot handle two revisions for the same repository URL '" + url
                                    + "': '" + foundScmVersion + "' and '" + srcVersion + "' of " + dep);
                        }
                    } else {
                        /* depBuild == null */
                        String id = repo.getId() + "-" + dep.getVersion();
                        Path projectRootDirectory = session.getTopLevelProject().getBasedir().toPath()
                                .resolve("target/srcdeps/" + id);
                        logger.info("using projectRootDirectory "+ projectRootDirectory.toString());

                        request = BuildRequest.builder() //
                                .projectRootDirectory(projectRootDirectory) //
                                .scmUrls(repo.getUrls()) //
                                .srcVersion(SrcVersion.parse(dep.getVersion())) //
                                .buildArguments(repo.getBuildArguments()) //
                                .verbosity(configuration.getVerbosity()) //
                                .ioRedirects(configuration.getRedirects()) //
                                .build();
                        depBuilds.put(url, request);
                    }
                }
            }
        }

        /* this could eventually be done in parallel */
        for (BuildRequest request : depBuilds.values()) {
            try {
                buildService.build(request);
            } catch (BuildException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
