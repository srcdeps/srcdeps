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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

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
            return new ScmRepository(id, Collections.unmodifiableList(selectors), Collections.unmodifiableList(urls),
                    Collections.unmodifiableList(buildArguments), skipTests, addDefaultBuildArguments);
        }

        public Builder buildArgument(String buildArgument) {
            this.buildArguments.add(buildArgument);
            return this;
        }

        public Builder buildArguments(List<String> buildArguments) {
            this.buildArguments.addAll(buildArguments);
            return this;
        }

        /**
         * Sets the {@link #id} after checking it using {@link ScmRepository#assertValidId(String)}.
         *
         * @param id
         *            the id string
         * @return this {@link Builder}
         */
        public Builder id(String id) {
            this.id = assertValidId(id);
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

    /** The period character that delimits the segments of {@link #id} values */
    public static final char ID_DELIMITER = '.';

    /**
     * Checks that the given {@code id} is a valid id. If no violation is found the given {@code id} is returned.
     * Otherwise an {@link IllegalArgumentException} is thrown.
     *
     * Valid IDs are much like Java packages: they are sequences of Java identifiers concatenad by {@code '.'}
     * character.
     *
     * @param id
     *            the ID to check
     * @return the {@code id} passed to this method
     * @throws IllegalArgumentException
     *             is the given {@code id} violates some of the requirements
     */
    public static String assertValidId(String id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s.id [%s]: cannot be null", ScmRepository.class.getSimpleName(), id));
        } else if (id.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s.id [%s]: cannot be empty", ScmRepository.class.getSimpleName(), id));
        } else if (id.charAt(0) == (ID_DELIMITER)) {
            throw new IllegalArgumentException(String.format("Invalid %s.id [%s]: cannot start with '.'",
                    ScmRepository.class.getSimpleName(), id));
        } else if (id.charAt(id.length() - 1) == (ID_DELIMITER)) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s.id [%s]: cannot end with '.'", ScmRepository.class.getSimpleName(), id));
        } else {
            int subsequentDelimiterCount = 0;
            for (int i = 0; i < id.length(); i++) {
                char ch = id.charAt(i);
                if (ch == ID_DELIMITER) {
                    subsequentDelimiterCount++;
                    if (subsequentDelimiterCount > 1) {
                        throw new IllegalArgumentException(String.format(
                                "Invalid %s.id [%s]: cannot contain mutiple delimiters ('.') one after another",
                                ScmRepository.class.getSimpleName(), id));
                    }
                } else if (subsequentDelimiterCount > 0 || i == 0) {
                    /* After the delimiter or at the very beginning */
                    if (!Character.isJavaIdentifierStart(ch)) {
                        throw new IllegalArgumentException(String.format(
                                "Invalid %s.id [%s]: Invalid character [%s] at position [%d]; a Java identifier start expected",
                                ScmRepository.class.getSimpleName(), id, ch, i));
                    } else {
                        subsequentDelimiterCount = 0;
                    }
                } else if (!Character.isJavaIdentifierPart(ch)) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid %s.id [%s]: Invalid character [%s] at position [%d]; a Java identifier part expected",
                            ScmRepository.class.getSimpleName(), id, ch, i));
                }
            }
        }
        return id;
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
     * @return an identifier of this {@link ScmRepository}. Should be a sequence of Java identifiers concatenated by
     *         {@code '.'} character
     */
    public String getId() {
        return id;
    }

    /**
     * @return a {@link Path} created out of {@code '.'} delimited segments of {@link #id}. If {@link #id} is
     *         {@code "org.project.component"} then the {@link Path} returned by this method will be
     *         {@code "org/project/component"}
     */
    public Path getIdAsPath() {
        List<String> pathElements = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(id, String.valueOf(ID_DELIMITER));
        String first = st.nextToken();
        while (st.hasMoreTokens()) {
            pathElements.add(st.nextToken());
        }
        return Paths.get(first, pathElements.toArray(new String[0]));
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
