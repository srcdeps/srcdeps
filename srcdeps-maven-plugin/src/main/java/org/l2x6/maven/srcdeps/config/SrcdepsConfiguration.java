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

        private final Xpp3Dom dom;

        private final PropsEvaluator evaluator;

        public Builder(Plugin plugin, Xpp3Dom dom, MavenSession session) {
            super();
            this.dom = dom;
            MojoExecution mojoExecution = new MojoExecution(plugin, "install", "whatever");
            this.evaluator = new PropsEvaluator(new PluginParameterExpressionEvaluator(session, mojoExecution));
        }

        public SrcdepsConfiguration build() {

            final boolean failOnMissingRepository = Optional
                    .ofNullable(dom.getChild(Element.failOnMissingRepository.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.createStringSupplier(Element.failOnMissingRepository)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.FALSE).value();

            final File sourcesDirectory = Optional.ofNullable(dom.getChild(Element.sourcesDirectory.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.createStringSupplier(Element.sourcesDirectory))
                    .orElseGet(evaluator.createStringSupplier("${settings.localRepository}/../dependency-sources"))
                    .map(Mapper.TO_FILE).value();

            final File javaHome = Optional.ofNullable(dom.getChild(Element.javaHome.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.createStringSupplier(Element.javaHome))
                    .orElseGet(evaluator.createStringSupplier("${java.home}")).map(Mapper.TO_FILE).value();

            final File mavenHome = Optional.ofNullable(dom.getChild(Element.mavenHome.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.createStringSupplier(Element.mavenHome))
                    .orElseGet(evaluator.createStringSupplier("${maven.home}")).map(Mapper.TO_FILE).value();

            final String scmPluginVersion = Optional.ofNullable(dom.getChild(Element.scmPluginVersion.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.createStringSupplier(Element.scmPluginVersion))
                    .orElseGet(new Supplier.Constant<String>("1.9.4")).value();

            final boolean skip = Optional.ofNullable(dom.getChild(Element.skip.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.createStringSupplier(Element.skip)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.FALSE).value();

            final boolean skipTests = Optional.ofNullable(dom.getChild(Element.skipTests.name())).map(Mapper.NODE_VALUE)
                    .orElseGet(evaluator.createStringSupplier(Element.skipTests)).map(Mapper.TO_BOOLEAN)
                    .orElseGet(Supplier.Constant.FALSE).value();

            final boolean mavenTestSkip = Optional.ofNullable(dom.getChild(Element.mavenTestSkip.name()))
                    .map(Mapper.NODE_VALUE).orElseGet(evaluator.createStringSupplier(Element.mavenTestSkip))
                    .map(Mapper.TO_BOOLEAN).orElseGet(Supplier.Constant.FALSE).value();

            Xpp3Dom reposElem = dom.getChild(Element.repositories.name());
            List<Repository> repos = new ArrayList<Repository>();
            if (reposElem != null) {
                Xpp3Dom[] reposElems = reposElem.getChildren(Element.repository.name());
                for (Xpp3Dom repoElem : reposElems) {
                    repos.add(Repository.load(repoElem, skipTests, mavenTestSkip));
                }
            }

            return new SrcdepsConfiguration(Collections.unmodifiableList(repos), failOnMissingRepository,
                    sourcesDirectory, mavenHome, javaHome, scmPluginVersion, skip, skipTests, mavenTestSkip);
        }

    }

    enum Element {
        failOnMissingRepository, id, javaHome, mavenHome, mavenTestSkip, repositories, repository, scmPluginVersion,
        selector, selectors, skip, skipTests, sourcesDirectory, url;

        public String toSrcDepsPropertyExpression() {
            return "${srcdeps." + toString() + "}";
        }
    }

    public interface Mapper<T, R> {
        Mapper<Xpp3Dom, String> NODE_VALUE = new Mapper<Xpp3Dom, String>() {
            @Override
            public String map(Xpp3Dom node) {
                return node != null ? node.getValue() : null;
            }
        };
        Mapper<String, Boolean> TO_BOOLEAN = new Mapper<String, Boolean>() {
            @Override
            public Boolean map(String value) {
                return value == null ? null : Boolean.valueOf(value);
            }
        };
        Mapper<String, File> TO_FILE = new Mapper<String, File>() {
            @Override
            public File map(String value) {
                return new File(value);
            }
        };

        R map(T t);
    }

    public static class Optional<T> {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        private static Optional<?> EMPTY = new Optional(null);

        @SuppressWarnings("unchecked")
        public static final <T> Optional<T> empty() {
            return (Optional<T>) EMPTY;
        }

        @SuppressWarnings("unchecked")
        public static <T> Optional<T> ofNullable(T value) {
            return value == null ? (Optional<T>) empty() : new Optional<T>(value);
        }

        private final T value;

        public Optional(T value) {
            super();
            this.value = value;
        }

        public boolean isPresent() {
            return value != null;
        }

        @SuppressWarnings("unchecked")
        public <U> Optional<U> map(Mapper<? super T, ? extends U> mapper) {
            if (!isPresent())
                return empty();
            else {
                return (Optional<U>) ofNullable(mapper.map(value));
            }
        }

        @SuppressWarnings("unchecked")
        public Optional<T> orElseGet(Supplier<? extends T> other) {
            if (!isPresent())
                return (Optional<T>) ofNullable(other.get());
            else {
                return this;
            }
        }

        public T value() {
            return value;
        }

    }

    public static class PropsEvaluator {
        private final PluginParameterExpressionEvaluator evaluator;

        public PropsEvaluator(PluginParameterExpressionEvaluator evaluator) {
            super();
            this.evaluator = evaluator;
        }

        public Supplier<String> createStringSupplier(final Element element) {
            return createStringSupplier(element.toSrcDepsPropertyExpression());
        }

        public Supplier<String> createStringSupplier(final String expression) {
            return new Supplier<String>() {
                @Override
                public String get() {
                    try {
                        return (String) evaluator.evaluate(expression);
                    } catch (ExpressionEvaluationException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

    }

    public interface Supplier<T> {
        public static class Constant<T> implements Supplier<T> {
            public static final Supplier<Boolean> FALSE = new Constant<Boolean>(Boolean.FALSE);
            private final T value;

            public Constant(T value) {
                super();
                this.value = value;
            }

            public T get() {
                return value;
            }
        }

        T get();
    }

    private final boolean failOnMissingRepository;
    private final File javaHome;
    private final File mavenHome;
    private final boolean mavenTestSkip;
    private final List<Repository> repositories;
    private final String scmPluginVersion;
    private final boolean skip;
    private final boolean skipTests;

    private final File sourcesDirectory;
    private SrcdepsConfiguration(List<Repository> repositories, boolean failOnMissingRepository, File sourcesDirectory,
            File mavenHome, File javaHome, String scmPluginVersion, boolean skipTests, boolean mavenTestSkip,
            boolean skip) {
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
        return "SrcdepsConfiguration [failOnMissingRepository=" + failOnMissingRepository + ", javaHome=" + javaHome
                + ", mavenHome=" + mavenHome + ", repositories=" + repositories + ", scmPluginVersion="
                + scmPluginVersion + ", sourcesDirectory=" + sourcesDirectory + ", skipTests=" + skipTests
                + ", mavenTestSkip=" + mavenTestSkip + "]";
    }
}
