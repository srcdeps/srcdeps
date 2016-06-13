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

/**
 * A Service for performing {@link BuildRequest}s. This will typically be the entry point for the users of the Core API.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface BuildService {

    /**
     * Performs the given {@code request}.
     *
     * @param request
     *            the request to build
     * @throws BuildException
     *             on any build related problem
     */
    void build(BuildRequest request) throws BuildException;
}
