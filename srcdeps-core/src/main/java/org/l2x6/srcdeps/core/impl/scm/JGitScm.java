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
import java.util.Collection;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.Scm;
import org.l2x6.srcdeps.core.ScmException;
import org.l2x6.srcdeps.core.SrcVersion;
import org.l2x6.srcdeps.core.SrcVersion.WellKnownType;
import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;
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
    private static final Logger log = LoggerFactory.getLogger(JGitScm.class);
    public static final String SCM_GIT_PREFIX = "git:";
    public static final String SRCDEPS_WORKING_BRANCH = "srcdeps-working-branch";

    /**
     * Tells if the given filesystem directory contains a valid git repository.
     *
     * @param dir
     *            a {@link Path} to check
     * @return {@code true} if the given {@code dir} contains a valid git repository; {@code false} otherwise.
     */
    private static boolean containsGitRepo(Path dir) {
        Path gitDir = dir.resolve(".git");
        if (!Files.exists(gitDir)) {
            return false;
        } else {
            try (FileRepository repo = new FileRepository(gitDir.toFile())) {
                return repo.getObjectDatabase().exists();
            } catch (IOException e) {
                log.warn(String.format("Could not check if [%s] contains a git repository", dir), e);
                /*
                 * We could perhaps throw e out of this method rather than return false. Returning false sounds as a
                 * better idea in case the repo is somehow damaged.
                 */
                return false;
            }
        }
    }

    private static String stripUriPrefix(String url) {
        return url.substring(SCM_GIT_PREFIX.length());
    }

    /**
     * Makes sure that the given {@code refToFind} is available in the {@code advertisedRefs}.
     *
     * @param advertisedRefs
     *            the {@link Collection} of {@link Ref}s to search in
     * @param refToFind
     *            the ref name to find
     * @param url
     *            the URL used to fetch
     * @throws ScmException
     *             if the given {@code refToFind} could not be found in the {@code advertisedRefs}
     */
    private void assertRefFetched(Collection<Ref> advertisedRefs, String refToFind, String url) throws ScmException {
        for (Ref ref : advertisedRefs) {
            if (refToFind.equals(ref.getName())) {
                return;
            }
        }
        throw new ScmException(String.format("Could not fetch ref [%s] from [%s]", refToFind, url));
    }

    /**
     * Walks back through the history of the {@code advertisedRefs} and tries to find the given {@code commitSha1}.
     *
     * @param repository
     *            the current {@link Repository} to search in
     * @param advertisedRefs
     *            the list of refs that were fetched and whose histories should be searched through
     * @param commitSha1
     *            the commit to find
     * @param url
     *            the URL that was used to fetch
     * @throws ScmException
     *             if the given {@code commitSha1} could not be found in the history of any of the
     *             {@code advertisedRefs}
     */
    private void assertRevisionFetched(Repository repository, Collection<Ref> advertisedRefs, String commitSha1,
            String url) throws ScmException {
        ObjectId needle = ObjectId.fromString(commitSha1);
        try {
            for (Ref ref : advertisedRefs) {

                try (RevWalk walk = new RevWalk(repository)) {
                    walk.markStart(walk.parseCommit(ref.getTarget().getObjectId()));
                    walk.setRetainBody(false);

                    for (RevCommit commit : walk) {
                        if (commit.getId().equals(needle)) {
                            return;
                        }
                    }
                }

            }
        } catch (IOException e) {
            new ScmException(String.format("Could not fetch ref [%s] from [%s]", commitSha1, url), e);
        }
        throw new ScmException(String.format("Could not fetch ref [%s] from [%s]", commitSha1, url));
    }

    /**
     * Checkout the source tree of a project to build, esp. using {@link BuildRequest#getScmUrls()} and
     * {@link BuildRequest#getSrcVersion()} of the given {@code request}.
     * <p>
     * This implementation first checks if {@code request.getProjectRootDirectory()} returns a directory containing a
     * valid git repository. If it does, git fetch operation is used. Otherwise, the repository is cloned.
     *
     * @param request
     *            determines the project to checkout
     * @throws ScmException
     *             on any SCM related problem
     * @see org.l2x6.srcdeps.core.Scm#checkout(org.l2x6.srcdeps.core.BuildRequest)
     */
    @Override
    public void checkout(BuildRequest request) throws ScmException {

        Path dir = request.getProjectRootDirectory();
        boolean dirExists = Files.exists(dir);
        if (dirExists && containsGitRepo(dir)) {
            /* there is a valid repo - try to fetch and reset */
            fetchAndReset(request);
        } else {
            /* there is no valid git repo in the directory */
            try {
                SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(dir);
            } catch (IOException e) {
                throw new ScmException(String.format("Srcdeps could not create directory [%s]", dir), e);
            }
            cloneAndCheckout(request);
        }
    }

    void cloneAndCheckout(BuildRequest request) throws ScmException {
        final Path dir = request.getProjectRootDirectory();

        final SrcVersion srcVersion = request.getSrcVersion();
        ScmException lastException = null;

        /* Try the urls one after another and exit on the first success */
        for (String url : request.getScmUrls()) {
            String useUrl = stripUriPrefix(url);
            log.info("Srcdeps attempting to clone version {} from SCM URL {}", request.getSrcVersion(), useUrl);

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
                log.warn("Srcdeps could not checkout version {} from SCM URL {}: {}: {}", request.getSrcVersion(),
                        useUrl, e.getClass().getName(), e.getMessage());
                lastException = new ScmException(String.format("Could not checkout from URL [%s]", useUrl), e);
            }
        }
        throw lastException;
    }

    void fetchAndReset(BuildRequest request) throws ScmException {
        final Path dir = request.getProjectRootDirectory();
        /* Forget local changes */
        try (Git git = Git.open(dir.toFile())) {
            Set<String> removedFiles = git.clean().setCleanDirectories(true).call();
            for (String removedFile : removedFiles) {
                log.debug("Srcdeps removed an unstaged file {}", removedFile);
            }
            git.reset().setMode(ResetType.HARD).call();

            /* make sure the srcdeps-working-branch exists */
            git.branchCreate().setName(SRCDEPS_WORKING_BRANCH).setForce(true).call();
            git.checkout().setName(SRCDEPS_WORKING_BRANCH).call();

        } catch (Exception e) {
            log.warn(String.format("Srcdeps could not forget local changes in [%s]", dir), e);
        }

        final SrcVersion srcVersion = request.getSrcVersion();
        ScmException lastException = null;
        int i = 0;
        for (String url : request.getScmUrls()) {
            String useUrl = stripUriPrefix(url);
            log.info("Srcdeps attempting to fetch version {} from SCM URL {}", request.getSrcVersion(), useUrl);
            String remoteAlias = i == 0 ? "origin" : "origin" + i;
            try (Git git = Git.open(dir.toFile())) {

                StoredConfig config = git.getRepository().getConfig();
                config.setString("remote", remoteAlias, "url", useUrl);
                config.save();

                final String startPoint;
                final String refToFetch;
                FetchCommand fetch = git.fetch().setRemote(remoteAlias);
                switch (srcVersion.getWellKnownType()) {
                case branch:
                    refToFetch = "refs/heads/" + srcVersion.getScmVersion();
                    fetch.setRefSpecs(new RefSpec(refToFetch));
                    startPoint = remoteAlias + "/" + srcVersion.getScmVersion();
                    break;
                case tag:
                    refToFetch = "refs/tags/" + srcVersion.getScmVersion();
                    fetch.setRefSpecs(new RefSpec(refToFetch));
                    startPoint = srcVersion.getScmVersion();
                    break;
                case revision:
                    refToFetch = null;
                    startPoint = srcVersion.getScmVersion();
                    break;
                default:
                    throw new IllegalStateException("Unexpected " + WellKnownType.class.getName() + " value '"
                            + srcVersion.getWellKnownType() + "'.");
                }
                FetchResult fetchResult = fetch.call();

                /*
                 * Let's check that the desired startPoint was really fetched from the current URL. Otherwise, the
                 * startPoint may come from an older fetch of the same repo URL (but was removed in between) or it may
                 * come from an older fetch of another URL. These cases may introduce situations when one developer can
                 * see a successful srcdep build (because he still has the outdated ref in his local git repo) but
                 * another dev with exectly the same setup cannot checkout because the ref is not there in any of the
                 * remote repos anymore.
                 */
                Collection<Ref> advertisedRefs = fetchResult.getAdvertisedRefs();
                switch (srcVersion.getWellKnownType()) {
                case branch:
                case tag:
                    assertRefFetched(advertisedRefs, refToFetch, url);
                    break;
                case revision:
                    assertRevisionFetched(git.getRepository(), advertisedRefs, srcVersion.getScmVersion(), url);
                    break;
                default:
                    throw new IllegalStateException("Unexpected " + WellKnownType.class.getName() + " value '"
                            + srcVersion.getWellKnownType() + "'.");
                }

                git.reset().setMode(ResetType.HARD).setRef(startPoint).call();
                return;
            } catch (ScmException e) {
                log.warn("Srcdeps could not checkout version {} from SCM URL {}: {}: {}", request.getSrcVersion(),
                        useUrl, e.getClass().getName(), e.getMessage());
                lastException = e;
            } catch (Exception e) {
                log.warn("Srcdeps could not checkout version {} from SCM URL {}: {}: {}", request.getSrcVersion(),
                        useUrl, e.getClass().getName(), e.getMessage());
                lastException = new ScmException(String.format("Could not checkout from URL [%s]", useUrl), e);
            }
            i++;
        }
        throw lastException;
    }

    @Override
    public boolean supports(String url) {
        return url.startsWith(SCM_GIT_PREFIX);
    }

}
