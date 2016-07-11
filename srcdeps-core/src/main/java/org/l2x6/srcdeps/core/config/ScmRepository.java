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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * A SCM repository entry of a {@link Configuration}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepository {

    public static class Builder {

        private List<String> buildArguments = new ArrayList<>();

        private String id;
        private List<String> selectors = new ArrayList<>();
        private Collection<String> urls = new LinkedHashSet<String>();
        public Builder() {
        }

        public ScmRepository build() {
            return new ScmRepository(id, Collections.unmodifiableList(selectors),
                    Collections.unmodifiableCollection(urls), Collections.unmodifiableList(buildArguments));
        }

        public Builder buildArguments(List<String> buildArgument) {
            this.buildArguments.addAll(buildArgument);
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder selectors(String selector) {
            this.selectors.add(selector);
            return this;
        }

        public Builder urls(String url) {
            this.urls.add(url);
            return this;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<String> buildArguments;
    private final String id;
    private final List<String> selectors;
    private final Collection<String> urls;

    private ScmRepository(String id, List<String> selectors, Collection<String> urls, List<String> buildArgs) {
        super();
        this.id = id;
        this.selectors = selectors;
        this.urls = urls;
        this.buildArguments = buildArgs;
    }

    public List<String> getBuildArguments() {
        return buildArguments;
    }

    public String getId() {
        return id;
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public Collection<String> getUrls() {
        return urls;
    }

    @Override
    public String toString() {
        return "ScmRepository [id=" + id + ", urls=" + urls + ", selectors=" + selectors + ", buildArguments="
                + buildArguments + "]";
    }

}
