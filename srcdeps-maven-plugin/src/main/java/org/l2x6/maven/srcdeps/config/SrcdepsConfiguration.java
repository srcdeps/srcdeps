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
package org.l2x6.maven.srcdeps.config;

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.SrcdepsPluginConstants;
import org.l2x6.maven.srcdeps.util.Mapper;
import org.l2x6.maven.srcdeps.util.Optional;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;
import org.l2x6.maven.srcdeps.util.Supplier;
import org.l2x6.srcdeps.core.BuildRequest.Verbosity;
import org.l2x6.srcdeps.core.shell.IoRedirects;

public class SrcdepsConfiguration {
    public static class Builder {

        private final Xpp3Dom dom;

        private final PropsEvaluator evaluator;

        private final Set<String> forwardProperties = new LinkedHashSet<String>();

        private final Logger logger;

        public Builder(PropsEvaluator evaluator, Xpp3Dom dom, MavenSession session, Logger logger) {
            super();
            this.dom = dom;
            this.evaluator = evaluator;
            this.logger = logger;

            Optional<String> rawFwdProps = evaluator.stringOptional(Element.forwardProperties);
            if (rawFwdProps.isPresent()) {
                String rawForwardProps = rawFwdProps.value();
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "srcdeps-maven-plugin srcdeps.forwardProperties was not null: [" + rawForwardProps + "]");
                }
                StringTokenizer st = new StringTokenizer(rawForwardProps, ", \t\n\r");
                while (st.hasMoreTokens()) {
                    String prop = st.nextToken().trim();
                    if (!prop.isEmpty()) {
                        forwardProperties.add(prop);
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("srcdeps-maven-plugin srcdeps.forwardProperties was null, using defaults");
                }
                for (Element elem : Element.values()) {
                    if (elem.forwardedByDefault) {
                        forwardProperties.add(elem.toSrcDepsProperty());
                    }
                }
                forwardProperties.add(Element.url.toSrcDepsProperty() + ".*");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("srcdeps-maven-plugin srcdeps.forwardProperties in effect: " + forwardProperties);
            }

        }

        public SrcdepsConfiguration build() {

            final boolean failOnMissingRepository = Optional
                    .ofNullable(dom.getChild(Element.failOnMissingRepository.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.failOnMissingRepository)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.TRUE).value();

            final File sourcesDirectory = Optional.ofNullable(dom.getChild(Element.sourcesDirectory.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.stringSupplier(Element.sourcesDirectory))
                    .orElseGet(evaluator.stringSupplier("${settings.localRepository}/../dependency-sources"))
                    .map(Mapper.TO_FILE).value();

            final File javaHome = Optional.ofNullable(dom.getChild(Element.javaHome.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.javaHome))
                    .orElseGet(evaluator.stringSupplier("${java.home}")).map(Mapper.TO_FILE).value();

            final File mavenHome = Optional.ofNullable(dom.getChild(Element.mavenHome.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.mavenHome))
                    .orElseGet(evaluator.stringSupplier("${maven.home}")).map(Mapper.TO_FILE).value();

            final String scmPluginVersion = Optional.ofNullable(dom.getChild(Element.scmPluginVersion.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.stringSupplier(Element.scmPluginVersion))
                    .orElseGet(new Supplier.Constant<String>("1.9.4")).value();

            final boolean skip = Optional.ofNullable(dom.getChild(Element.skip.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.skip)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.FALSE).value();

            final boolean skipTests = Optional.ofNullable(dom.getChild(Element.skipTests.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.skipTests)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.FALSE).value();

            final boolean mavenTestSkip = Optional.ofNullable(dom.getChild(Element.mavenTestSkip.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.stringSupplier(Element.mavenTestSkip))
                    .map(Mapper.TO_BOOLEAN).orElseGet(Supplier.Constant.FALSE).value();

            final String verbosity = Optional.ofNullable(dom.getChild(Element.verbosity.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.stringSupplier(Element.verbosity))
                    .orElseGet(new Supplier.Constant<String>("default")).value();

            final Redirect input = IoRedirects.parseUri( //
                    Optional.ofNullable(dom.getChild(Element.builderInput.name())) //
                            .map(Mapper.NODE_VALUE) //
                            .orElseGet(new Supplier.Constant<String>("inherit")) //
                            .value());
            final Redirect output = IoRedirects.parseUri( //
                    Optional.ofNullable(dom.getChild(Element.builderOutput.name())) //
                    .map(Mapper.NODE_VALUE) //
                    .orElseGet(new Supplier.Constant<String>("inherit")) //
                    .value());
            final Redirect error = IoRedirects.parseUri( //
                    Optional.ofNullable(dom.getChild(Element.builderError.name())) //
                    .map(Mapper.NODE_VALUE) //
                    .orElseGet(new Supplier.Constant<String>("inherit")) //
                    .value());
            final IoRedirects redirects = new IoRedirects(input, output, error);

            final Set<String> failWithProfiles = Optional.ofNullable(dom.getChild(Element.failWithProfiles.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.stringSupplier(Element.failWithProfiles))
                    .map(Mapper.TO_STRING_SET)
                    .orElseGet(new Supplier.Constant<Set<String>>(SrcdepsPluginConstants.DEFAULT_FAIL_WITH_PROFILES))
                    .value();

            Xpp3Dom reposElem = dom.getChild(Element.repositories.name());
            List<SrcdepsRepository> repos = new ArrayList<SrcdepsRepository>();
            if (reposElem != null) {
                Xpp3Dom[] reposElems = reposElem.getChildren(Element.repository.name());
                for (Xpp3Dom repoElem : reposElems) {
                    repos.add(SrcdepsRepository.load(repoElem, skipTests, mavenTestSkip, evaluator, logger));
                }
            }

            return new SrcdepsConfiguration(Collections.unmodifiableList(repos), failOnMissingRepository,
                    sourcesDirectory, mavenHome, javaHome, scmPluginVersion, skipTests, mavenTestSkip, skip,
                    Verbosity.ofId(verbosity), redirects, forwardProperties, failWithProfiles);
        }

    }

    public enum Element {
        buildArgs, //
        builderError, //
        builderInput, //
        builderOutput, //
        failOnMissingRepository(true), //
        failWithProfiles, //
        forwardProperties, //
        id, //
        javaHome(true), //
        mavenHome(true), //
        mavenTestSkip(true), //
        repositories, //
        repository, //
        scmPluginVersion(true), //
        selector, //
        selectors, //
        skip(true), //
        skipTests(true), //
        sourcesDirectory(true), //
        url, //
        verbosity(true);

        private final boolean forwardedByDefault;

        private Element() {
            this(false);
        }

        private Element(boolean forwardedByDefault) {
            this.forwardedByDefault = forwardedByDefault;
        }

        public String toSrcDepsProperty() {
            return "srcdeps." + toString();
        }

        public String toSrcDepsPropertyExpression() {
            return "${srcdeps." + toString() + "}";
        }
    }

    private final boolean failOnMissingRepository;
    private final Set<String> failWithProfiles;
    private final Set<String> forwardProperties;
    private final File javaHome;
    private final File mavenHome;
    private final boolean mavenTestSkip;
    private final IoRedirects redirects;
    private final List<SrcdepsRepository> repositories;
    private final String scmPluginVersion;
    private final boolean skip;
    private final boolean skipTests;
    private final File sourcesDirectory;
    private final Verbosity verbosity;

    private SrcdepsConfiguration(List<SrcdepsRepository> repositories, boolean failOnMissingRepository,
            File sourcesDirectory, File mavenHome, File javaHome, String scmPluginVersion, boolean skipTests,
            boolean mavenTestSkip, boolean skip, Verbosity verbosity, IoRedirects redirects,
            Set<String> forwardProperties, Set<String> failWithProfiles) {
        super();
        this.repositories = repositories;
        this.failOnMissingRepository = failOnMissingRepository;
        this.sourcesDirectory = sourcesDirectory;
        this.mavenHome = mavenHome;
        this.javaHome = javaHome;
        this.scmPluginVersion = scmPluginVersion;
        this.skip = skip;
        this.skipTests = skipTests;
        this.mavenTestSkip = mavenTestSkip;
        this.verbosity = verbosity;
        this.forwardProperties = forwardProperties;
        this.failWithProfiles = failWithProfiles;
        this.redirects = redirects;
    }

    public Set<String> getFailWithProfiles() {
        return failWithProfiles;
    }

    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    public File getJavaHome() {
        return javaHome;
    }

    public File getMavenHome() {
        return mavenHome;
    }

    public IoRedirects getRedirects() {
        return redirects;
    }

    public List<SrcdepsRepository> getRepositories() {
        return repositories;
    }

    public String getScmPluginVersion() {
        return scmPluginVersion;
    }

    public File getSourcesDirectory() {
        return sourcesDirectory;
    }

    public Verbosity getVerbosity() {
        return verbosity;
    }

    public boolean isFailOnMissingRepository() {
        return failOnMissingRepository;
    }

    public boolean isMavenTestSkip() {
        return mavenTestSkip;
    }

    public boolean isSkip() {
        return skip;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "SrcdepsConfiguration [failOnMissingRepository=" + failOnMissingRepository + ", failWithProfiles="
                + failWithProfiles + ", forwardProperties=" + forwardProperties + ", javaHome=" + javaHome
                + ", mavenHome=" + mavenHome + ", mavenTestSkip=" + mavenTestSkip + ", repositories=" + repositories
                + ", scmPluginVersion=" + scmPluginVersion + ", skip=" + skip + ", skipTests=" + skipTests
                + ", sourcesDirectory=" + sourcesDirectory + ", verbosity=" + verbosity + "]";
    }

}
