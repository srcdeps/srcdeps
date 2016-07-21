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
package org.l2x6.srcdeps.localrepo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Provider;

import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.l2x6.srcdeps.config.yaml.YamlConfigurationIo;
import org.l2x6.srcdeps.core.BuildException;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.BuildService;
import org.l2x6.srcdeps.core.SrcVersion;
import org.l2x6.srcdeps.core.config.BuilderIo;
import org.l2x6.srcdeps.core.config.Configuration;
import org.l2x6.srcdeps.core.config.ConfigurationException;
import org.l2x6.srcdeps.core.config.ScmRepository;
import org.l2x6.srcdeps.core.shell.IoRedirects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LocalRepositoryManager} able to build the requested artifacts from their sources.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsLocalRepositoryManager implements LocalRepositoryManager {

    /**
     * Encapsulates the loading of {@link Configuration} that has to be done lazily - i.e. at some point when
     * {@link MavenSession} is available already.
     */
    private static class LazyConfiguration {
        private final Provider<MavenSession> sessionProvider;

        public LazyConfiguration(Provider<MavenSession> sessionProvider) {
            super();
            this.sessionProvider = sessionProvider;
        }

        private volatile Configuration configuration;
        private final Object configurationLock = new Object();

        /**
         * Loads the {@link Configuration} lazily. If multiple threads call this method while {@link #configuration} is
         * uninitialized, only the first one loads from the file system while others are waiting.
         *
         * @return a {@link Configuration}
         */
        public Configuration getConfiguration() {
            synchronized (configurationLock) {
                if (configuration == null) {
                    File srcdepsYaml = locateSrcdepsYaml();
                    log.debug("SrcdepsLocalRepositoryManager using {}", srcdepsYaml.getAbsolutePath());
                    try (Reader r = new InputStreamReader(new FileInputStream(srcdepsYaml), "utf-8")) {
                        configuration = new YamlConfigurationIo().read(r);
                    } catch (IOException | ConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return configuration;
        }

        /**
         * Finds the {@link ScmRepository} associated with the given {@code artifact}. The association is given by the
         * exact string match between the groupId of the {@code artifact} and one of the
         * {@link ScmRepository#getSelectors() selectors} of {@link ScmRepository}
         *
         * @param artifact
         * @return
         */
        public ScmRepository findScmRepo(Artifact artifact) {
            final String groupId = artifact.getGroupId();
            for (ScmRepository scmRepository : getConfiguration().getRepositories()) {
                if (scmRepository.getSelectors().contains(groupId)) {
                    return scmRepository;
                }
            }
            throw new IllegalStateException(String
                    .format("No srcdeps SCM repository configured in .mvn/srcdeps.yaml for groupId [%s]", groupId));
        }

        /**
         * Looks up the base directory of the current Maven request and tries to resolve {@code ".mvn/srcdeps.yaml"}
         * against it or against its closest parent directory.
         *
         * @return
         */
        public File locateSrcdepsYaml() {
            MavenSession mavenSession = sessionProvider.get();
            String baseDir = mavenSession.getRequest().getBaseDirectory();
            for (Path basePath = Paths.get(baseDir); basePath != null; basePath = basePath.getParent()) {
                Path result = basePath.resolve(relativeMvnSrcdepsYaml);
                if (Files.exists(result)) {
                    return result.toFile();
                }
            }
            throw new RuntimeException(
                    String.format("Could not locate [%s] starting at path [%s]", relativeMvnSrcdepsYaml, baseDir));
        }

    }

    private static final Logger log = LoggerFactory.getLogger(SrcdepsLocalRepositoryManager.class);
    public static final Path relativeMvnSrcdepsYaml = Paths.get(".mvn", "srcdeps.yaml");

    private final BuildService buildService;
    private final Path scrdepsDir;
    private final LocalRepositoryManager delegate;

    private LazyConfiguration configuration;

    public SrcdepsLocalRepositoryManager(LocalRepositoryManager delegate, Provider<MavenSession> sessionProvider,
            BuildService buildService) {
        super();
        this.delegate = delegate;
        this.buildService = buildService;
        this.scrdepsDir = delegate.getRepository().getBasedir().toPath().getParent().resolve("srcdeps");
        this.configuration = new LazyConfiguration(sessionProvider);
    }

    /**
     * Delegated to {@link #delegate}
     *
     * @see org.eclipse.aether.repository.LocalRepositoryManager#add(org.eclipse.aether.RepositorySystemSession,
     *      org.eclipse.aether.repository.LocalArtifactRegistration)
     */
    @Override
    public void add(RepositorySystemSession session, LocalArtifactRegistration request) {
        delegate.add(session, request);
    }

    /**
     * Delegated to {@link #delegate}
     *
     * @see org.eclipse.aether.repository.LocalRepositoryManager#add(org.eclipse.aether.RepositorySystemSession,
     *      org.eclipse.aether.repository.LocalMetadataRegistration)
     */
    @Override
    public void add(RepositorySystemSession session, LocalMetadataRegistration request) {
        delegate.add(session, request);
    }

    /**
     * In case the {@link #delegate} does not find the given artifact and the given artifact's version string is a
     * srcdeps version string, then the version is built from source and returned.
     *
     * @see org.eclipse.aether.repository.LocalRepositoryManager#find(org.eclipse.aether.RepositorySystemSession,
     *      org.eclipse.aether.repository.LocalArtifactRequest)
     */
    @Override
    public LocalArtifactResult find(RepositorySystemSession session, LocalArtifactRequest request) {
        final LocalArtifactResult result = delegate.find(session, request);

        Artifact artifact = request.getArtifact();
        String version = artifact.getVersion();
        if (!result.isAvailable() && SrcVersion.isSrcVersion(version)) {
            Configuration config = configuration.getConfiguration();

            ScmRepository scmRepo = configuration.findScmRepo(artifact);
            Path projectBuildDir = scrdepsDir.resolve(scmRepo.getId());

            BuilderIo builderIo = config.getBuilderIo();
            IoRedirects ioRedirects = IoRedirects.builder() //
                    .stdin(IoRedirects.parseUri(builderIo.getStdin())) //
                    .stdout(IoRedirects.parseUri(builderIo.getStdout())) //
                    .stderr(IoRedirects.parseUri(builderIo.getStderr())) //
                    .build();
            BuildRequest buildRequest = BuildRequest.builder() //
                    .projectRootDirectory(projectBuildDir) //
                    .scmUrls(scmRepo.getUrls()) //
                    .srcVersion(SrcVersion.parse(version)) //
                    .buildArguments(scmRepo.getBuildArguments()) //
                    .skipTests(scmRepo.isSkipTests())
                    .addDefaultBuildArguments(scmRepo.isAddDefaultBuildArguments())
                    .verbosity(config.getVerbosity()) //
                    .ioRedirects(ioRedirects) //
                    .build();
            try {
                buildService.build(buildRequest);

                /* check once again if the delegate sees the newly built artifact */
                final LocalArtifactResult newResult = delegate.find(session, request);
                if (!newResult.isAvailable()) {
                    log.error(
                            "Srcdeps build succeeded but the artifact {} is still not available in the local repository",
                            artifact);
                }
            } catch (BuildException e) {
                log.error("Srcdeps could not build " + request, e);
            }
        }

        return result;
    }

    @Override
    public LocalMetadataResult find(RepositorySystemSession session, LocalMetadataRequest request) {
        return delegate.find(session, request);
    }

    @Override
    public String getPathForLocalArtifact(Artifact artifact) {
        return delegate.getPathForLocalArtifact(artifact);
    }

    @Override
    public String getPathForLocalMetadata(Metadata metadata) {
        return delegate.getPathForLocalMetadata(metadata);
    }

    @Override
    public String getPathForRemoteArtifact(Artifact artifact, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteArtifact(artifact, repository, context);
    }

    @Override
    public String getPathForRemoteMetadata(Metadata metadata, RemoteRepository repository, String context) {
        return delegate.getPathForRemoteMetadata(metadata, repository, context);
    }

    @Override
    public LocalRepository getRepository() {
        return delegate.getRepository();
    }

}
