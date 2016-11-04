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

import org.l2x6.srcdeps.core.impl.scm.JGitScm;

/**
 * An abstraction of a revision control system, such as Git, Subversion, etc.
 * <p>
 * {@link #supports(String)} is used to auto-detect the {@link Scm} implementation to use for the given kind of URLs.
 * The implementations decide based on a prefix of the URL - e.g. {@link JGitScm} expects the URLs to start with
 * {@code git:} as in {@code git:https://github.com/srcdeps/srcdeps-test-artifact.git}. This prefix is supposed to be
 * removed by the {@link Scm} implementation when actually checking out. Hence the effective URL that will be used by
 * {@link JGitScm} to checkout is {@code https://github.com/srcdeps/srcdeps-test-artifact.git}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface Scm {

    /**
     * Checkout the source tree of a project to build, esp. using {@link BuildRequest#getScmUrls()} and
     * {@link BuildRequest#getSrcVersion()} of the given {@code request}.
     *
     * @param request
     *            determines the project to checkout
     * @throws ScmException
     *             on any SCM related problem
     */
    void checkout(BuildRequest request) throws ScmException;

    /**
     * @param url
     *            the URL to decide about
     * @return {@code true} if the present {@link Scm} implementation can checkout from the given {@code url},
     *         {@code false} otherwise.
     */
    boolean supports(String url);
}
