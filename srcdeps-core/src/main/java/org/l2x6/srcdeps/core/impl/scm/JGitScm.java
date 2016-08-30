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
package org.l2x6.srcdeps.core.impl.scm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.util.FileUtils;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.Scm;
import org.l2x6.srcdeps.core.ScmException;
import org.l2x6.srcdeps.core.SrcVersion;
import org.l2x6.srcdeps.core.SrcVersion.WellKnownType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JGit based implementation of a Git {@link Scm}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class JGitScm implements Scm {
    public static final String SCM_GIT_PREFIX = "git:";
    private static final Logger log = LoggerFactory.getLogger(JGitScm.class);

    @Override
    public void checkout(BuildRequest request) throws ScmException {

        Path dir = request.getProjectRootDirectory();
        if (Files.exists(dir)) {
            // TODO: Rather than deleting every time, we should consider fetch/reset if the dir already contains a git repo
            try {
                // Courtesy https://dev.eclipse.org/mhonarc/lists/jgit-dev/msg01957.html
                FileUtils.delete(dir.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
            } catch (IOException e) {
                throw new ScmException(String.format("Srcdeps could not delete directory [%s]", dir), e);
            }
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ScmException(String.format("Srcdeps could not create directory [%s]", dir), e);
        }

        ScmException lastException = null;

        /* Try the urls one after another and exit on the first success */
        for (String url : request.getScmUrls()) {
            String useUrl = url.substring(SCM_GIT_PREFIX.length());
            log.info("Attempting to checkout version {} from SCM URL {}", request.getSrcVersion(), useUrl);
            SrcVersion srcVersion = request.getSrcVersion();

            CloneCommand cmd = Git.cloneRepository().setURI(useUrl).setDirectory(dir.toFile());

            switch (srcVersion.getWellKnownType()) {
            case branch:
            case tag:
                cmd.setBranch(srcVersion.getScmVersion());
                break;
            case revision:
                cmd.setCloneAllBranches(true);
                break;
            default:
                throw new IllegalStateException("Unexpected " + WellKnownType.class.getName() + " value '"
                        + srcVersion.getWellKnownType() + "'.");
            }

            try (Git git = cmd.call()) {
                git.checkout().setName(srcVersion.getScmVersion()).call();

                /*
                 * workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=474093
                 */
                git.getRepository().close();

                /* return on the first success */
                return;
            } catch (Exception e) {
                log.warn("Could not checkout version {} from SCM URL {}: {}: {}", request.getSrcVersion(), useUrl,
                        e.getClass().getName(), e.getMessage());
                lastException = new ScmException(String.format("Could not checkout from URL [%s]", useUrl), e);
            }
        }
        throw lastException;
    }

    @Override
    public boolean supports(String url) {
        return url.startsWith(SCM_GIT_PREFIX);
    }

}
