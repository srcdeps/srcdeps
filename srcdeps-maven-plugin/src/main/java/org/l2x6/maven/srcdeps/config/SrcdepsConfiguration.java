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
package org.l2x6.maven.srcdeps.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class SrcdepsConfiguration {
    public static class Builder {
        private static String childValue(Xpp3Dom parentNode, Element childName, String defaultValue) {
            if (parentNode != null) {
                Xpp3Dom childNode = parentNode.getChild(childName.name());
                if (childNode != null) {
                    String val = childNode.getValue();
                    if (val != null) {
                        return val;
                    }
                }
            }
            return defaultValue;
        }

        private final Xpp3Dom dom;

        private final PluginParameterExpressionEvaluator evaluator;

        public Builder(Plugin plugin, Xpp3Dom dom, MavenSession session) {
            super();
            this.dom = dom;
            MojoExecution mojoExecution = new MojoExecution(plugin, "install", "whatever");
            this.evaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);
        }

        public SrcdepsConfiguration build() {

            Xpp3Dom reposElem = dom.getChild(Element.repositories.name());
            List<Repository> repos = new ArrayList<Repository>();
            if (reposElem != null) {
                Xpp3Dom[] reposElems = reposElem.getChildren(Element.repository.name());
                for (Xpp3Dom repoElem : reposElems) {
                    repos.add(Repository.load(repoElem));
                }
            }

            try {
                String strMissingRepo = (String) evaluator.evaluate("${srcdeps.failOnMissingRepository}");
                boolean failOnMissingRepository = false;
                if (strMissingRepo != null) {
                    failOnMissingRepository = Boolean.parseBoolean(strMissingRepo);
                } else {
                    failOnMissingRepository = Boolean
                            .parseBoolean(childValue(dom, Element.failOnMissingRepository, Boolean.FALSE.toString()));
                }

                File sourcesDirectory = null;
                String srcDir = (String) evaluator.evaluate("${srcdeps.sourcesDirectory}");
                childValue(dom, Element.sourcesDirectory, null);
                if (srcDir != null) {
                    sourcesDirectory = new File(srcDir);
                } else {
                    sourcesDirectory = new File(
                            (String) eval("${srcdeps.sourcesDirectory}",
                                    "${settings.localRepository}/../dependency-sources"));
                }

                File javaHome = null;
                String srcJavaHome = (String) evaluator.evaluate("${srcdeps.javaHome}");
                childValue(dom, Element.javaHome, null);
                if (srcDir != null) {
                    javaHome = new File(srcJavaHome);
                } else {
                    javaHome = new File((String) evaluator.evaluate("${java.home}"));
                }

                File mavenHome = null;
                String srcMavenHome = (String) evaluator.evaluate("${srcdeps.mavenHome}");
                childValue(dom, Element.mavenHome, null);
                if (srcDir != null) {
                    mavenHome = new File(srcMavenHome);
                } else {
                    mavenHome = new File((String) evaluator.evaluate("${maven.home}"));
                }

                String scmPluginVersion = (String) evaluator.evaluate("${srcdeps.scmPluginVersion}");
                if (scmPluginVersion == null) {
                    scmPluginVersion = "1.9.4";
                }

                return new SrcdepsConfiguration(Collections.unmodifiableList(repos), failOnMissingRepository,
                        sourcesDirectory, mavenHome, javaHome, scmPluginVersion);
            } catch (ExpressionEvaluationException e) {
                throw new RuntimeException(e);
            }
        }

        private Object eval(String expression, String fallbackExpression) {
            try {
                Object result = evaluator.evaluate(expression);
                if (result != null) {
                    return result;
                } else {
                    return evaluator.evaluate(fallbackExpression);
                }
            } catch (ExpressionEvaluationException e) {
                throw new RuntimeException(e);
            }
        }

    }

    enum Element {
        failOnMissingRepository, id, javaHome, mavenHome, repositories, repository, scmPluginVersion, selector,
        selectors, sourcesDirectory, url;
    }

    private final boolean failOnMissingRepository;
    private final File javaHome;
    private final File mavenHome;
    private final List<Repository> repositories;
    private final String scmPluginVersion;

    private final File sourcesDirectory;

    private SrcdepsConfiguration(List<Repository> repositories, boolean failOnMissingRepository, File sourcesDirectory,
            File mavenHome, File javaHome, String scmPluginVersion) {
        super();
        this.repositories = repositories;
        this.failOnMissingRepository = failOnMissingRepository;
        this.sourcesDirectory = sourcesDirectory;
        this.mavenHome = mavenHome;
        this.javaHome = javaHome;
        this.scmPluginVersion = scmPluginVersion;
    }

    public File getJavaHome() {
        return javaHome;
    }

    public File getMavenHome() {
        return mavenHome;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public String getScmPluginVersion() {
        return scmPluginVersion;
    }

    public File getSourcesDirectory() {
        return sourcesDirectory;
    }

    public boolean isFailOnMissingRepository() {
        return failOnMissingRepository;
    }

    @Override
    public String toString() {
        return "SrcdepsConfiguration [failOnMissingRepository=" + failOnMissingRepository + ", javaHome=" + javaHome
                + ", mavenHome=" + mavenHome + ", repositories=" + repositories + ", scmPluginVersion="
                + scmPluginVersion + ", sourcesDirectory=" + sourcesDirectory + "]";
    }
}
