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
package org.srcdeps.core.impl.scm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.ScmException;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JGitScmTest {

    private static final Path targetDir = Paths.get(System.getProperty("project.build.directory", "target"))
            .toAbsolutePath();

    @Test
    public void testCheckout() throws IOException, ScmException, NoHeadException, GitAPIException {
        Path dir = targetDir.resolve("test-repo");
        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(dir);

        /* first clone */
        BuildRequest cloningRequest = BuildRequest.builder().srcVersion(SrcVersion.parse("0.0.1-SRC-tag-0.0.1"))
                .projectRootDirectory(dir).scmUrl("git:https://github.com/srcdeps/srcdeps-test-artifact.git").build();
        JGitScm jGitScm = new JGitScm();

        jGitScm.checkout(cloningRequest);

        /* ensure that the tag is there through checking that it has a known commit hash */

        assertCommit(dir, "19ef91ed30fd8b1a459803ee0c279dcf8e236184");

        /* try if the fetch works after we have cloned already */
        BuildRequest fetchingRequest = BuildRequest.builder()
                .srcVersion(SrcVersion.parse("0.0.1-SRC-revision-0a5ab902099b24c2b13ed1dad8c5f537458bcc89"))
                .projectRootDirectory(dir).scmUrl("git:https://github.com/srcdeps/srcdeps-test-artifact.git").build();

        jGitScm.fetchAndReset(fetchingRequest);

        /* ensure that the WC's HEAD has the known commit hash */
        assertCommit(dir, "0a5ab902099b24c2b13ed1dad8c5f537458bcc89");

    }

    private void assertCommit(Path dir, String expectedSha1) throws IOException, NoHeadException, GitAPIException {
        try (Git git = Git.open(dir.toFile())) {
            Iterable<RevCommit> history = git.log().call();
            String foundSha1 = history.iterator().next().getName();
            Assert.assertEquals(String.format("Git repository in [%s] not at the expected revision", dir), expectedSha1,
                    foundSha1);
        }

    }
}
