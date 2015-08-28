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
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;

import edu.emory.mathcs.backport.java.util.Arrays;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "srcdeps")
public class SrcdepsLifecycleParticipant extends AbstractMavenLifecycleParticipant {

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
        logger.info("SrcdepsLifecycleParticipant");

        List<String> goals = session.getGoals();

        logger.info("goals = " + goals);
        if (goals != null && shouldTriggerSrcdepsBuild(goals)) {
            List<MavenProject> projects = session.getProjects();
            logger.info("SrcdepsLifecycleParticipant projects = "+ projects);

            for (MavenProject project : projects) {
                logger.info("srcdeps for project "+ project.getGroupId() +":"+ project.getArtifactId());
                Plugin plugin = findSrcdepsPlugin(project);
                if (plugin != null) {
                    Object conf = plugin.getConfiguration();
                    if (conf instanceof Xpp3Dom) {
                        SrcdepsConfiguration srcdepsConfiguration = new SrcdepsConfiguration.Builder(plugin,
                                (Xpp3Dom) conf, session).build();
                        @SuppressWarnings("unchecked")
                        Map<Dependency, ScmVersion> revisions = filterSrcdeps(project.getDependencies());
                        new SrcdepsInstaller(session, logger, artifactHandlerManager, srcdepsConfiguration,
                                revisions).install();
                    }
                }
            }
        }
    }

    private Map<Dependency, ScmVersion> filterSrcdeps(List<Dependency> deps) {
        Map<Dependency, ScmVersion> revisions = new HashMap<Dependency, ScmVersion>();
        logger.info("About to check " + deps.size() + " compile dependencies");
        for (Dependency dep : deps) {
            ScmVersion scmVersion = ScmVersion.fromSrcdepsVersionString(dep.getVersion());
            logger.info("Got source revision '" + scmVersion + "' from " + dep);
            if (scmVersion != null) {
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
        logger.info("checking if any of " + goals + " is in " + TRIGGER_PHASES);
        for (String goal : goals) {
            if (TRIGGER_PHASES.contains(goal)) {
                return true;
            }
        }
        return false;
    }

}
