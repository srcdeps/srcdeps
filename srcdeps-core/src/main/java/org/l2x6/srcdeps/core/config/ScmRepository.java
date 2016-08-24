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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2x6.srcdeps.core.config.Configuration.Builder;

/**
 * A SCM repository entry of a {@link Configuration}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepository {

    public static class Builder {

        private boolean addDefaultBuildArguments = true;
        private List<String> buildArguments = new ArrayList<>();
        private String id;
        private List<String> selectors = new ArrayList<>();
        private boolean skipTests = true;
        private List<String> urls = new ArrayList<String>();

        public Builder() {
        }

        public Builder addDefaultBuildArguments(boolean addDefaultBuildArguments) {
            this.addDefaultBuildArguments = addDefaultBuildArguments;
            return this;
        }

        public ScmRepository build() {
            return new ScmRepository(id, Collections.unmodifiableList(selectors),
                    Collections.unmodifiableList(urls), Collections.unmodifiableList(buildArguments), skipTests,
                    addDefaultBuildArguments);
        }

        public Builder buildArgument(String buildArgument) {
            this.buildArguments.add(buildArgument);
            return this;
        }

        public Builder buildArguments(List<String> buildArguments) {
            this.buildArguments.addAll(buildArguments);
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder selector(String selector) {
            this.selectors.add(selector);
            return this;
        }

        public Builder selectors(List<String> selectors) {
            this.selectors.addAll(selectors);
            return this;
        }

        public Builder skipTests(boolean skipTests) {
            this.skipTests = skipTests;
            return this;
        }

        public Builder url(String url) {
            this.urls.add(url);
            return this;
        }

        public Builder urls(List<String> urls) {
            this.urls.addAll(urls);
            return this;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final boolean addDefaultBuildArguments;
    private final List<String> buildArguments;
    private final String id;
    private final List<String> selectors;
    private final boolean skipTests;
    private final List<String> urls;

    private ScmRepository(String id, List<String> selectors, List<String> urls, List<String> buildArgs,
            boolean skipTests, boolean addDefaultBuildArguments) {
        super();
        this.id = id;
        this.selectors = selectors;
        this.urls = urls;
        this.buildArguments = buildArgs;
        this.skipTests = skipTests;
        this.addDefaultBuildArguments = addDefaultBuildArguments;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScmRepository other = (ScmRepository) obj;
        if (addDefaultBuildArguments != other.addDefaultBuildArguments)
            return false;
        if (buildArguments == null) {
            if (other.buildArguments != null)
                return false;
        } else if (!buildArguments.equals(other.buildArguments))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (selectors == null) {
            if (other.selectors != null)
                return false;
        } else if (!selectors.equals(other.selectors))
            return false;
        if (skipTests != other.skipTests)
            return false;
        if (urls == null) {
            if (other.urls != null)
                return false;
        } else if (!urls.equals(other.urls))
            return false;
        return true;
    }

    public List<String> getBuildArguments() {
        return buildArguments;
    }

    /**
     * @return an identifier of this {@link ScmRepository}. Only {@code [a-zA-Z0-9_]} characters are allowed.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns a {@link List} of selectors to map dependency artifacts to source repositories. ATM, the association is
     * given by the exact string match between the {@code groupId} of the dependency artifact and one of the selectors
     * listed here.
     *
     * @return a {@link List} of selectors
     */
    public List<String> getSelectors() {
        return selectors;
    }

    /**
     * Returns a {@link List} of SCM URLs to checkout the sources of the given dependency. If multiple SCM repos are
     * returned then only the first successful checkout should count.
     *
     * @return a {@link List} of SCM URLs to checkout the sources of the given dependency
     */
    public List<String> getUrls() {
        return urls;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (addDefaultBuildArguments ? 1231 : 1237);
        result = prime * result + ((buildArguments == null) ? 0 : buildArguments.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((selectors == null) ? 0 : selectors.hashCode());
        result = prime * result + (skipTests ? 1231 : 1237);
        result = prime * result + ((urls == null) ? 0 : urls.hashCode());
        return result;
    }

    /**
     * If {@code true} the build tool's default arguments will be used when building a dependency. Otherwise, no default
     * build arguments will be used. The default build arguments are build tool specific. For Maven, the default build
     * arguments are defined in {@link org.l2x6.srcdeps.core.impl.builder.AbstractMvnBuilder#mvnDefaultArgs}.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isAddDefaultBuildArguments() {
        return addDefaultBuildArguments;
    }

    /**
     * If {@code true} no tests will be run when building a dependency. For dependencies built with Maven, this accounts
     * to adding {@code -DskipTests} to the {@code mvn} arguments.
     *
     * @return {@code true} or {@code false}
     */
    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "ScmRepository [id=" + id + ", urls=" + urls + ", selectors=" + selectors + ", buildArguments="
                + buildArguments + "]";
    }

}
