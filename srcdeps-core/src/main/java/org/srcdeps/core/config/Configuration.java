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
package org.srcdeps.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.srcdeps.core.BuildRequest.Verbosity;

/**
 * A configuration suitable for a build system wanting to handle source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Configuration {
    public static class Builder {

        private BuilderIo builderIo = BuilderIo.inheritAll();
        private Set<String> forwardProperties = new LinkedHashSet<>(defaultForwardProperties);
        private List<ScmRepository> repositories = new ArrayList<>();
        private boolean skip = false;
        private Path sourcesDirectory;
        private Verbosity verbosity = Verbosity.warn;

        private Builder() {
            super();
        }

        public Configuration build() {
            return new Configuration(repositories, sourcesDirectory, skip, verbosity, builderIo,
                    forwardProperties);
        }

        public Builder builderIo(BuilderIo.Builder builderIo) {
            this.builderIo = builderIo.build();
            return this;
        }

        public Builder configModelVersion(String configModelVersion) {
            if (!CONFIG_MODEL_VERSION.equals(configModelVersion)) {
                throw new IllegalArgumentException(String.format("Cannot parse configModelVersion [%s]; expected [%s]",
                        configModelVersion, CONFIG_MODEL_VERSION));
            }
            return this;
        }

        public Builder forwardProperties(Collection<String> values) {
            forwardProperties.addAll(values);
            return this;
        }

        public Builder forwardProperty(String value) {
            forwardProperties.add(value);
            return this;
        }

        public Builder repositories(Map<String, ScmRepository.Builder> repoBuilders) {
            for (Map.Entry<String, ScmRepository.Builder> en : repoBuilders.entrySet()) {
                ScmRepository.Builder repoBuilder = en.getValue();
                repoBuilder.id(en.getKey());
                this.repositories.add(repoBuilder.build());
            }
            return this;
        }

        public Builder repository(ScmRepository repo) {
            this.repositories.add(repo);
            return this;
        }

        public Builder skip(boolean value) {
            this.skip = value;
            return this;
        }

        public Builder sourcesDirectory(Path value) {
            this.sourcesDirectory = value;
            return this;
        }

        public Builder verbosity(Verbosity value) {
            this.verbosity = value;
            return this;
        }

    }
    public static final String CONFIG_MODEL_VERSION = "1.0";

    public static final List<String> defaultForwardProperties = Collections.singletonList("srcdeps.mvn.*");

    public static final String SRCDEPS_MVN_SETTINGS_PROP = "srcdeps.mvn.settings";

    public static Builder builder() {
        return new Builder();
    }
    private final BuilderIo builderIo;
    private final Set<String> forwardProperties;
    private final List<ScmRepository> repositories;
    private final boolean skip;
    private final Path sourcesDirectory;

    private final Verbosity verbosity;

    private Configuration(List<ScmRepository> repositories, Path sourcesDirectory, boolean skip,
            Verbosity verbosity, BuilderIo redirects, Set<String> forwardProperties) {
        super();
        this.repositories = repositories;
        this.sourcesDirectory = sourcesDirectory;
        this.skip = skip;
        this.verbosity = verbosity;
        this.forwardProperties = forwardProperties;
        this.builderIo = redirects;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Configuration other = (Configuration) obj;
        if (builderIo == null) {
            if (other.builderIo != null)
                return false;
        } else if (!builderIo.equals(other.builderIo))
            return false;
        if (forwardProperties == null) {
            if (other.forwardProperties != null)
                return false;
        } else if (!forwardProperties.equals(other.forwardProperties))
            return false;
        if (repositories == null) {
            if (other.repositories != null)
                return false;
        } else if (!repositories.equals(other.repositories))
            return false;
        if (skip != other.skip)
            return false;
        if (sourcesDirectory == null) {
            if (other.sourcesDirectory != null)
                return false;
        } else if (!sourcesDirectory.equals(other.sourcesDirectory))
            return false;
        if (verbosity != other.verbosity)
            return false;
        return true;
    }

    /**
     * @return a {@link BuilderIo} to use for the child builders
     */
    public BuilderIo getBuilderIo() {
        return builderIo;
    }

    /**
     * Returns a set of property names that the top level builder A should pass as java system properties to every
     * dependency builder B (using {@code -DmyProperty=myValue}) command line arguments. Further, in case a child
     * builder B spawns its own new child builder C, B must pass all these properties to C in the very same manner as A
     * did to B.
     * <p>
     * A property name may end with asterisk {@code *} to denote that all properties starting with the part before the
     * asterisk should be forwared. E.g. {@code my.prop.*} would forward both {@code my.prop.foo} and
     * {@code my.prop.bar}.
     *
     * @return a {@link Set} of property names
     */
    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    /**
     * @return the list of {@link ScmRepository ScmRepositories} that should be used for building of the dependencies
     */
    public List<ScmRepository> getRepositories() {
        return repositories;
    }

    /**
     * Returns a {@link Path} to the sources directory where the dependency sources should be checked out. Each SCM
     * repository will have a subdirectory named after its {@code id} there.
     *
     * @return a {@link Path} to the sources
     */
    public Path getSourcesDirectory() {
        return sourcesDirectory;
    }

    /**
     * Returns the verbosity level the appropriate dependency build tool (such as Maven) should use during the build of
     * a dependency. The interpretation of the individual levels is up to the given build tool. Some build tools may
     * map the levels listed here to a distinct set of levels they support internally.
     *
     * @return the verbosity level
     */
    public Verbosity getVerbosity() {
        return verbosity;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((builderIo == null) ? 0 : builderIo.hashCode());
        result = prime * result + ((forwardProperties == null) ? 0 : forwardProperties.hashCode());
        result = prime * result + ((repositories == null) ? 0 : repositories.hashCode());
        result = prime * result + (skip ? 1231 : 1237);
        result = prime * result + ((sourcesDirectory == null) ? 0 : sourcesDirectory.hashCode());
        result = prime * result + ((verbosity == null) ? 0 : verbosity.hashCode());
        return result;
    }

    /**
     * @return {@code true} if the whole srcdeps processing should be skipped or {@code false} otherwise
     */
    public boolean isSkip() {
        return skip;
    }

    @Override
    public String toString() {
        return "SrcdepsConfig [forwardProperties=" + forwardProperties + ", builderIo=" + builderIo + ", repositories="
                + repositories + ", skip=" + skip + ", sourcesDirectory="
                + sourcesDirectory + ", verbosity=" + verbosity + "]";
    }

}
