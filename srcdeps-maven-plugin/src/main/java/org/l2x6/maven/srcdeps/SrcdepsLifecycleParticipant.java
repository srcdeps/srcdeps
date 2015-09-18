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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

import edu.emory.mathcs.backport.java.util.Arrays;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "srcdeps")
public class SrcdepsLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static class Gav {
        public static Gav ofDependency(Dependency dep) {
            return new Gav(dep.getGroupId(), dep.getArtifactId(), dep.getVersion());
        }

        public static Gav ofModel(Model model) {
            return new Gav(model.getGroupId(), model.getArtifactId(), model.getVersion());
        }

        private final String artifactId;
        private final String groupId;
        private final String version;

        public Gav(String groupId, String artifactId, String version) {
            super();
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Gav other = (Gav) obj;
            if (artifactId == null) {
                if (other.artifactId != null)
                    return false;
            } else if (!artifactId.equals(other.artifactId))
                return false;
            if (groupId == null) {
                if (other.groupId != null)
                    return false;
            } else if (!groupId.equals(other.groupId))
                return false;
            if (version == null) {
                if (other.version != null)
                    return false;
            } else if (!version.equals(other.version))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
            result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
            result = prime * result + ((version == null) ? 0 : version.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "GAV [groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + "]";
        }

    }

    @SuppressWarnings("unchecked")
    private static Set<String> TRIGGER_PHASES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(new String[] { "validate", "initialize", "generate-sources", "process-sources",
                    "generate-resources", "process-resources", "compile", "process-classes", "generate-test-sources",
                    "process-test-sources", "generate-test-resources", "process-test-resources", "test-compile",
                    "process-test-classes", "test", "prepare-package", "package", "pre-integration-test",
                    "integration-test", "post-integration-test", "verify", "install", "deploy" })));

    @Requirement
    private ArtifactHandlerManager artifactHandlerManager;

    @Requirement
    private Logger logger;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        boolean globalSkip = Boolean
                .valueOf(session.getUserProperties().getProperty(Element.skip.toSrcDepsProperty(), "false"));
        if (globalSkip) {
            logger.info("srcdeps-maven-plugin skipped");
            return;
        }

        List<String> goals = session.getGoals();
        if (goals != null && shouldTriggerSrcdepsBuild(goals)) {
            List<MavenProject> projects = session.getProjects();
            logger.debug("SrcdepsLifecycleParticipant projects = " + projects);

            Set<Gav> projectGavs = new HashSet<SrcdepsLifecycleParticipant.Gav>();
            for (MavenProject project : projects) {
                projectGavs.add(Gav.ofModel(project.getModel()));
            }

            for (MavenProject project : projects) {
                logger.info("srcdeps-maven-plugin scanning project " + project.getGroupId() + ":"
                        + project.getArtifactId());
                Plugin plugin = findSrcdepsPlugin(project);
                if (plugin != null) {
                    Object conf = plugin.getConfiguration();
                    if (conf instanceof Xpp3Dom) {
                        MojoExecution mojoExecution = new MojoExecution(plugin, "install", "whatever");
                        PropsEvaluator evaluator = new PropsEvaluator(
                                new PluginParameterExpressionEvaluator(session, mojoExecution));
                        SrcdepsConfiguration srcdepsConfiguration = new SrcdepsConfiguration.Builder(evaluator,
                                (Xpp3Dom) conf, session, logger).build();
                        if (srcdepsConfiguration.isSkip()) {
                            logger.info("srcdeps-maven-plugin skipped for project " + project.getGroupId() + ":"
                                    + project.getArtifactId());
                        } else {
                            @SuppressWarnings("unchecked")
                            Map<Dependency, ScmVersion> revisions = filterSrcdeps(project.getDependencies(),
                                    projectGavs);
                            new SrcdepsInstaller(session, evaluator, logger, artifactHandlerManager,
                                    srcdepsConfiguration, revisions).install();
                        }
                    }
                }
            }
        }
    }

    private Map<Dependency, ScmVersion> filterSrcdeps(List<Dependency> deps, Set<Gav> projects) {
        Map<Dependency, ScmVersion> revisions = new HashMap<Dependency, ScmVersion>();
        logger.debug("srcdeps-maven-plugin scanning " + deps.size() + " compile dependencies");
        for (Dependency dep : deps) {
            ScmVersion scmVersion = ScmVersion.fromSrcdepsVersionString(dep.getVersion());
            logger.debug("Got source revision '" + scmVersion + "' from " + dep);
            if (scmVersion != null && !projects.contains(Gav.ofDependency(dep))) {
                revisions.put(dep, scmVersion);
            }
        }
        revisions = Collections.unmodifiableMap(revisions);
        return revisions;
    }

    private Plugin findSrcdepsPlugin(MavenProject project) {
        @SuppressWarnings("unchecked")
        List<Dependency> deps = project.getDependencies();
        @SuppressWarnings("unchecked")
        List<Plugin> plugins = project.getBuildPlugins();
        if (plugins != null && deps != null) {
            for (Plugin plugin : plugins) {

                if (SrcdepsConstants.ORG_L2X6_MAVEN_SRCDEPS_GROUP_ID.equals(plugin.getGroupId())
                        && SrcdepsConstants.SRCDEPS_MAVEN_PLUGIN_ADRTIFACT_ID.equals(plugin.getArtifactId())) {
                    return plugin;
                }
            }
        }
        return null;
    }

    private boolean shouldTriggerSrcdepsBuild(List<String> goals) {
        for (String goal : goals) {
            if (TRIGGER_PHASES.contains(goal)) {
                logger.info("srcdeps-maven-plugin triggered by goal [" + goal + "]");
                return true;
            }
        }
        logger.info("srcdeps-maven-plugin not triggered by any of the goals [" + goals + "]");
        return false;
    }

}
