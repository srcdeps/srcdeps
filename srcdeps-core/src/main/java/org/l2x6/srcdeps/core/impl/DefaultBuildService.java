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
package org.l2x6.srcdeps.core.impl;

import java.nio.file.Path;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.l2x6.srcdeps.core.BuildException;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.BuildService;
import org.l2x6.srcdeps.core.Builder;
import org.l2x6.srcdeps.core.Scm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default implementation of {@link BuildService} that makes use of the {@link Builder}s and {@link Scm}s injected
 * by the DI container.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class DefaultBuildService implements BuildService {
    private static final Logger log = LoggerFactory.getLogger(DefaultBuildService.class);
    private final Set<Builder> builders;
    private final Set<Scm> scms;

    @Inject
    public DefaultBuildService(Set<Builder> builders, Set<Scm> scms) {
        super();
        this.builders = builders;
        this.scms = scms;
    }

    @Override
    public void build(BuildRequest request) throws BuildException {
        final Path dir = request.getProjectRootDirectory();
        final String firstUrl = request.getScmUrls().iterator().next();
        log.info("About to build request {}", request);
        boolean checkedOut = false;
        for (Scm scm : scms) {
            if (scm.supports(firstUrl)) {
                log.info("About to use Scm implementation {} to check out URL {} to directory {}",
                        scm.getClass().getName(), firstUrl, dir);
                scm.checkout(request);
                checkedOut = true;
                break;
            }
        }
        if (!checkedOut) {
            throw new BuildException(String.format("No Scm found for URL [%s]", firstUrl));
        }

        boolean built = false;
        for (Builder builder : builders) {
            if (builder.canBuild(dir)) {
                log.info("About to build project in {} using Builder {}", dir, builder.getClass().getName());
                builder.setVersions(request);
                builder.build(request);
                built = true;
                break;
            }
        }
        if (!built) {
            throw new BuildException(String.format("No Builder found for directory [%s]", dir));
        }

    }

}
