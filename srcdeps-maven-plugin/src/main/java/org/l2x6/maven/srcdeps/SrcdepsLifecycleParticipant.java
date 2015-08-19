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
import java.util.List;
import java.util.Map;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.ProjectDependencyGraph;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "srcdeps")
public class SrcdepsLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    @Requirement
    private Logger logger;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        ProjectDependencyGraph graph = session.getProjectDependencyGraph();
        logger.info("ProjectDependencyGraph = " + (graph != null ? graph.toString() : "null"));

        MavenProject project = session.getCurrentProject();
        @SuppressWarnings("unchecked")
        List<Dependency> deps = project.getDependencies();

        @SuppressWarnings("unchecked")
        List<Plugin> plugins = project.getBuildPlugins();
        if (plugins != null && deps != null) {
            for (Plugin plugin : plugins) {

                if (SrcdepsConstants.ORG_L2X6_MAVEN_SRCDEPS_GROUP_ID.equals(plugin.getGroupId())
                        && SrcdepsConstants.SRCDEPS_MAVEN_PLUGIN_ADRTIFACT_ID.equals(plugin.getArtifactId())) {

                    Object conf = plugin.getConfiguration();
                    if (conf instanceof Xpp3Dom) {
                        SrcdepsConfiguration srcdepsConfiguration = new SrcdepsConfiguration.Builder((Xpp3Dom) conf,
                                session).build();
                        Map<Dependency, String> revisions = filterSrcdeps(deps);
                        new SrcdepsInstaller(session, srcdepsConfiguration, revisions).install();
                    }
                }

            }
        }

    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {

    }

    @SuppressWarnings("unchecked")
    private Map<Dependency, String> filterSrcdeps(List<Dependency> deps) {
        Map<Dependency, String> revisions = new HashMap<Dependency, String>();
        logger.info("About to check " + deps.size() + " compile dependencies");
        for (Dependency dep : deps) {
            String srcRevision = SrcdepsUtils.getSourceRevision(dep.getVersion());
            logger.info("Got source revision '" + srcRevision + "' from " + dep);
            if (srcRevision != null) {
                revisions.put(dep, srcRevision);
            }
        }
        revisions = Collections.unmodifiableMap(revisions);
        return revisions;
    }

}
