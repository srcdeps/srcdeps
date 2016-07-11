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
package org.l2x6.srcdeps.core.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2x6.srcdeps.core.BuildRequest.Verbosity;

/**
 * A configuration suitable for a build system wanting to handle source dependencies.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class Configuration {
    public static String CONFIG_MODEL_VERSION = "1.0";

    public static class Builder {

        private Set<String> forwardProperties = new LinkedHashSet<>();
        private BuilderIo builderIo;
        private List<ScmRepository> repositories = new ArrayList<>();
        private boolean skip = false;
        private boolean skipTests = true;
        private Path sourcesDirectory;
        private Verbosity verbosity = Verbosity.warn;

        private Builder() {
            super();
        }

        public Configuration build() {
            return new Configuration(repositories, sourcesDirectory, skipTests, skip, verbosity, builderIo,
                    forwardProperties);
        }

        public Builder forwardProperty(String value) {
            forwardProperties.add(value);
            return this;
        }

        public Builder forwardProperties(Collection<String> values) {
            forwardProperties.addAll(values);
            return this;
        }

        public Builder builderIo(BuilderIo.Builder builderIo) {
            this.builderIo = builderIo.build();
            return this;
        }

        public Builder skip(boolean value) {
            this.skip = value;
            return this;
        }

        public Builder skipTests(boolean value) {
            this.skipTests = value;
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

        public Builder repositories(Map<String, ScmRepository.Builder> repoBuilders) {
            for (Map.Entry<String, ScmRepository.Builder> en : repoBuilders.entrySet()) {
                ScmRepository.Builder repoBuilder = en.getValue();
                repoBuilder.id(en.getKey());
                this.repositories.add(repoBuilder.build());
            }
            return this;
        }

        public Builder configModelVersion(String configModelVersion) {
            if (!CONFIG_MODEL_VERSION.equals(configModelVersion)) {
                throw new IllegalArgumentException(String.format("Cannot parse configModelVersion [%s]; expected [%s]",
                        configModelVersion, CONFIG_MODEL_VERSION));
            }
            return this;
        }

    }

    private final Set<String> forwardProperties;
    private final BuilderIo builderIo;
    private final List<ScmRepository> repositories;
    private final boolean skip;
    private final boolean skipTests;
    private final Path sourcesDirectory;
    private final Verbosity verbosity;

    private Configuration(List<ScmRepository> repositories, Path sourcesDirectory, boolean skipTests, boolean skip,
            Verbosity verbosity, BuilderIo redirects, Set<String> forwardProperties) {
        super();
        this.repositories = repositories;
        this.sourcesDirectory = sourcesDirectory;
        this.skip = skip;
        this.skipTests = skipTests;
        this.verbosity = verbosity;
        this.forwardProperties = forwardProperties;
        this.builderIo = redirects;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> getForwardProperties() {
        return forwardProperties;
    }

    public BuilderIo getBuilderIo() {
        return builderIo;
    }

    public List<ScmRepository> getRepositories() {
        return repositories;
    }

    public Path getSourcesDirectory() {
        return sourcesDirectory;
    }

    public Verbosity getVerbosity() {
        return verbosity;
    }

    public boolean isSkip() {
        return skip;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "SrcdepsConfig [forwardProperties=" + forwardProperties + ", builderIo=" + builderIo + ", repositories="
                + repositories + ", skip=" + skip + ", skipTests=" + skipTests + ", sourcesDirectory="
                + sourcesDirectory + ", verbosity=" + verbosity + "]";
    }

}
