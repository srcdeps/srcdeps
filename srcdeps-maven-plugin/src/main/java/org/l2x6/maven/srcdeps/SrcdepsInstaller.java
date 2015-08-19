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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.apache.maven.shared.release.exec.ForkedMavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.l2x6.maven.srcdeps.config.Repository;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.MojoExecutor.Element;

public class SrcdepsInstaller {
    private final SrcdepsConfiguration configuration;
    private final Map<Dependency, String> revisions;
    private final MavenSession session;

    public SrcdepsInstaller(MavenSession session, SrcdepsConfiguration configuration,
            Map<Dependency, String> revisions) {
        super();
        this.configuration = configuration;
        this.revisions = revisions;
        this.session = session;
    }

    protected void build(DependencyBuild depBuild) throws MojoExecutionException {

        MavenExecutor mavenExecutor = new ForkedMavenExecutor();
        ReleaseEnvironment releaseEnvironment = new DefaultReleaseEnvironment().setJavaHome(configuration.getJavaHome())
                .setMavenHome(configuration.getJavaHome()).setSettings(session.getSettings());
        try {
            mavenExecutor.executeGoals(depBuild.getWorkingDirectory(), "versions:set", releaseEnvironment, false,
                    "-D newVersion=" + depBuild.getVersion(), "pom.xml", new ReleaseResult());

            mavenExecutor.executeGoals(depBuild.getWorkingDirectory(), "clean install", releaseEnvironment, false, null,
                    "pom.xml", new ReleaseResult());

        } catch (MavenExecutorException e) {
            throw new RuntimeException("Could not build " + depBuild.getWorkingDirectory(), e);
        }

    }

    protected void checkout(DependencyBuild depBuild) throws MojoExecutionException {
        File checkoutDir = depBuild.getWorkingDirectory();
        if (!checkoutDir.exists()) {
            checkoutDir.mkdirs();
        }
        BuildPluginManager pluginManager;
        try {
            pluginManager = (BuildPluginManager) session.lookup(BuildPluginManager.class.getName());
        } catch (ComponentLookupException e) {
            throw new RuntimeException(e);
        }
        String srcAbsPath = configuration.getSourcesDirectory().getAbsolutePath();
        Element[] config = new MojoExecutor.Element[] { MojoExecutor.element(MojoExecutor.name("basedir"), srcAbsPath),
                MojoExecutor.element(MojoExecutor.name("checkoutDirectory"), srcAbsPath),
                MojoExecutor.element(MojoExecutor.name("connectionUrl"), depBuild.getUrl()),
                MojoExecutor.element(MojoExecutor.name("scmVersionType"), "revision"),
                MojoExecutor.element(MojoExecutor.name("scmVersion"), depBuild.getRevisionId()) };
        MojoExecutor.executeMojo(MojoExecutor.plugin(MojoExecutor.groupId("org.apache.maven.plugins"),
                MojoExecutor.artifactId("maven-scm-plugin"), MojoExecutor.version(configuration.getScmPluginVersion())),
                MojoExecutor.goal("checkout"), MojoExecutor.configuration(config),
                MojoExecutor.executionEnvironment(session.getCurrentProject(), session, pluginManager));
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
        Map<String, DependencyBuild> depBuilds = new HashMap<String, DependencyBuild>(revisions.size());
        for (Map.Entry<Dependency, String> revisionEntry : revisions.entrySet()) {
            Dependency dep = revisionEntry.getKey();
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

        /* this could eventually be done in parallel */
        for (DependencyBuild depBuild : depBuilds.values()) {
            try {
                checkout(depBuild);
                build(depBuild);
            } catch (MojoExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
