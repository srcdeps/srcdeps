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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.codehaus.plexus.util.FileUtils;
import org.l2x6.maven.srcdeps.ScmVersion.ScmVersionElement;
import org.l2x6.maven.srcdeps.config.Repository;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;
import org.l2x6.maven.srcdeps.util.Optional;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;
import org.l2x6.maven.srcdeps.util.Supplier;

public class SrcdepsInstaller {

    private static class ArgsBuilder {
        private final StringBuilder builder = new StringBuilder();
        private final SrcdepsConfiguration configuration;
        private final PropsEvaluator evaluator;
        private final Logger logger;
        private final MavenSession session;

        public ArgsBuilder(SrcdepsConfiguration configuration, MavenSession session, PropsEvaluator evaluator,
                Logger logger) {
            super();
            this.configuration = configuration;
            this.evaluator = evaluator;
            this.logger = logger;
            this.session = session;
            if (configuration.isQuiet()) {
                opt("-q");
            }
        }

        public String build() {
            return builder.length() == 0 ? null : builder.toString();
        }

        public ArgsBuilder buildOptions() {
            String rawFwdProps = evaluator.stringOptional(Element.forwardProperties)
                    .orElseGet(Supplier.Constant.EMPTY_STRING).value();
            property(Element.forwardProperties.toSrcDepsProperty(), rawFwdProps);

            Set<String> useProps = new LinkedHashSet<String>(configuration.getForwardProperties());
            for (String prop : configuration.getForwardProperties()) {
                if (prop.endsWith("*")) {
                    /* prefix */
                    String prefix = prop.substring(prop.length() - 1);
                    for (Object key : session.getUserProperties().keySet()) {
                        if (key instanceof String && ((String) key).startsWith(prefix)) {
                            useProps.add((String) key);
                        }
                    }
                    for (Object key : session.getSystemProperties().keySet()) {
                        if (key instanceof String && ((String) key).startsWith(prefix)) {
                            useProps.add((String) key);
                        }
                    }
                }
            }

            for (String prop : useProps) {
                String expression = "${" + prop + "}";
                Optional<String> value = evaluator.stringOptional(expression);
                if (value.isPresent()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("srcdeps-maven-plugin  evaluated " + expression + " as " + value.value());
                    }
                    property(prop, value.value());
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("srcdeps-maven-plugin  is not forwarding property " + prop
                                + " because it has no value");
                    }
                }
            }

            if (configuration.isMavenTestSkip()) {
                property("maven.test.skip", String.valueOf(configuration.isMavenTestSkip()));
            }
            if (configuration.isSkipTests()) {
                property("skipTests", String.valueOf(configuration.isSkipTests()));
            }

            return this;
        }

        public ArgsBuilder nonDefaultProp(String key, boolean value, boolean defaultValue) {
            if (defaultValue != value) {
                property(key, String.valueOf(value));
            }
            return this;
        }

        public ArgsBuilder opt(String opt) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            // FIXME: we should check and eventually quote and/or escape the the
            // whole param
            builder.append(opt);
            return this;
        }

        public ArgsBuilder profiles(List<String> profiles) {
            if (profiles != null && !profiles.isEmpty()) {
                if (builder.length() != 0) {
                    builder.append(' ');
                }
                // FIXME: we should check and eventually quote and/or escape the
                // the
                // whole param
                builder.append("-P");
                boolean first = true;
                for (String profile : profiles) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(',');
                    }
                    builder.append(profile);
                }
            }
            return this;
        }

        public ArgsBuilder properties(Map<String, String> props) {
            for (Map.Entry<String, String> en : props.entrySet()) {
                property(en.getKey(), en.getValue());
            }
            return this;
        }

        public ArgsBuilder property(String key, String value) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            builder.append("\"-D").append(key).append('=').append(value).append('\"');
            return this;
        }
    }

    private final ArtifactHandlerManager artifactHandlerManager;

    private final SrcdepsConfiguration configuration;
    private final PropsEvaluator evaluator;
    private final boolean isWindows;
    private final Logger logger;
    private final MavenExecutor mavenExecutor;
    private final ReleaseEnvironment releaseEnvironment;
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

    protected void build(DependencyBuild depBuild) throws MavenExecutorException {
        logger.info(
                "srcdeps-maven-plugin is setting [" + depBuild.getId() + "] version [" + depBuild.getVersion() + "]");

        final String versionsArgs = new ArgsBuilder(configuration, session, evaluator, logger)
                .property("newVersion", depBuild.getVersion()).property("generateBackupPoms", "false").build();
        execute(depBuild.getWorkingDirectory(), "versions:set", versionsArgs);

        logger.info(
                "srcdeps-maven-plugin is building [" + depBuild.getId() + "] version [" + depBuild.getVersion() + "]");
        final String buildArgs = new ArgsBuilder(configuration, session, evaluator, logger)
                .profiles(depBuild.getProfiles()) //
                .buildOptions() //
                .properties(depBuild.getProperties()) //
                .build();
        execute(depBuild.getWorkingDirectory(), depBuild.getGoalsString(), buildArgs);
    }

    protected void checkout(DependencyBuild depBuild) throws MavenExecutorException {

        Collection<String> urls = depBuild.getUrls();
        int i = 0;
        MavenExecutorException executorException = null;
        File checkoutDir = depBuild.getWorkingDirectory();
        if (checkoutDir.exists()) {
            try {
                logger.debug("srcdeps-maven-plugin is about to delete [" + checkoutDir.getAbsolutePath() + "]");
                FileUtils.deleteDirectory(checkoutDir);
            } catch (IOException e) {
                throw new MavenExecutorException(
                        "srcdeps-maven-plugin could not delete [" + checkoutDir.getAbsolutePath() + "]", e);
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
            logger.info("srcdeps-maven-plugin is checking out [" + depBuild.getId() + "] version ["
                    + depBuild.getVersion() + "] from URL [" + i + "] [" + url + "]");
            i++;

            final ScmVersion scmVersion = depBuild.getScmVersion();

            // logger.info("Using args ["+ args +"]");

            try {
                for (ScmVersionElement element : scmVersion.getElements()) {
                    final String args = new ArgsBuilder(configuration, session, evaluator, logger)
                            .property("checkoutDirectory", checkoutDir.getAbsolutePath()).property("connectionUrl", url)
                            .property("scmVersionType", element.getVersionType())
                            .property("scmVersion", element.getVersion())
                            .nonDefaultProp("skipTests", depBuild.isSkipTests(), false)
                            .nonDefaultProp("maven.test.skip", depBuild.isMavenTestSkip(), false).build();
                    execute(new File(session.getExecutionRootDirectory()), "org.apache.maven.plugins:maven-scm-plugin:"
                            + configuration.getScmPluginVersion() + ":checkout", args);
                }

                /* break the url loop on first success */
                return;
            } catch (MavenExecutorException e) {
                logger.info("srcdeps-maven-plugin could not check out [" + depBuild.getId() + "] version ["
                        + depBuild.getVersion() + "] from URL [" + i + "] [" + url + "] : " + e.getMessage());
                executorException = e;
            }
        }

        if (executorException != null) {
            throw executorException;
        }

    }

    private void execute(File workingDirectory, String goals, String args) throws MavenExecutorException {
        if (isWindows) {
            logger.info("srcdeps-maven-plugin invoking cd " + workingDirectory.getAbsolutePath() + "; mvn " + goals
                    + " " + args);
        }
        mavenExecutor.executeGoals(workingDirectory, goals, releaseEnvironment, false, args, "pom.xml",
                new ReleaseResult());
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

    public void install() {
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
                                repo.isMavenTestSkip(), repo.getGoals(), repo.getProfiles(), repo.getProperties());
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
