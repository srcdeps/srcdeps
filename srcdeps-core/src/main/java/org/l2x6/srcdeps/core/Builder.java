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
package org.l2x6.srcdeps.core;

import java.nio.file.Path;

/**
 * An abstraction of a build tool such as Maven or Gradle.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Builder {
    /**
     * Build the project as required in the given {@code request}.
     *
     * @param request
     *            the request to build
     * @throws BuildException
     *             on any build related problem
     */
    void build(BuildRequest request) throws BuildException;

    /**
     * Returns {@code true} if the project in the given {@code projectRootDirectory} can be built by this
     * {@link Builder}, {@code false} otherwise. This method is used to auto-select a {@link Builder} implementation for
     * the given project.
     *
     * @param projectRootDirectory
     *            the root directory of the project's source tree
     * @return {@code true} if the project in the given {@code projectRootDirectory} can be built by this
     *         {@link Builder} , {@code false} otherwise.
     */
    boolean canBuild(Path projectRootDirectory);

    /**
     * Sets the versions in {@code pom.xml} files or other files as appropriate for the project named in the given
     * {@code request}.
     *
     * @param request
     *            the request to build
     * @throws BuildException
     *             on any build related problem
     */
    void setVersions(BuildRequest request) throws BuildException;

}
