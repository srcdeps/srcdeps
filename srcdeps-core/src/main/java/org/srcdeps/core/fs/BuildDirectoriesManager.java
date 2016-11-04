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
package org.srcdeps.core.fs;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.BuildException;
import org.srcdeps.core.SrcVersion;
import org.srcdeps.core.config.ScmRepository;
import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A class responsible (i) for the directory layout under {@link #rootDirectory} and (ii) for requesting an exclusive
 * lock for a given build directory. The locking is delegated to {@link PathLocker}.
 * <p>
 * The {@link #rootDirectory} is typically <code>${maven.repo.local}/../srcdeps</code>. Under that directory, each
 * project to build gets a subdirectory that corresponds to the relative {@link Path} returned by
 * {@link ScmRepository#getIdAsPath()}. Hence if the {@code id} if the repository is {@code "org.project.component"}
 * then its build home will be {@code "${rootDirectory}/org/project/component"}. Under this build home, the
 * {@link #openBuildDirectory(Path)} method opens up to {@link #CONCURRENCY_THRESHOLD} subdirectories, as required by
 * the concurrent load of the present machine.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuildDirectoriesManager {

    /**
     * The maximal number of subdirectories that {@link #openBuildDirectory(Path)} is allowed to create under the
     * project build home. This number should set some reasonable upper bound that when reached, signals that there is a
     * bug in our code. The value is {@value #CONCURRENCY_THRESHOLD}.
     */
    private static final int CONCURRENCY_THRESHOLD = 256;

    private static final Logger log = LoggerFactory.getLogger(BuildDirectoriesManager.class);

    private final PathLocker<SrcVersion> pathLocker;
    private final Path rootDirectory;

    public BuildDirectoriesManager(Path rootDirectory, PathLocker<SrcVersion> pathLocker) {
        super();
        this.rootDirectory = rootDirectory;
        this.pathLocker = pathLocker;
    }

    /**
     * Goes sequentially over integers form {@code 0} to {@link #CONCURRENCY_THRESHOLD} until it finds such {@code i} of
     * them which when appended to <code>"${rootDirectory}/${projectBuildHome}"</code>, makes up a new or existing
     * directory <code>"${rootDirectory}/${projectBuildHome}/${i}"</code> that can be locked using {@link #pathLocker}.
     * The {@link PathLock} returned contains a reference to the first
     * <code>"${rootDirectory}/${projectBuildHome}/${i}"</code> that could be locked successfully.
     * <p>
     * The returned {@link PathLock} should be released using its {@link Closeable#close()} method.
     *
     * @param projectBuildHome
     *            the given project's build home (something like {@code Paths.get("org", "project", "component")})
     *            relative to {@link #rootDirectory} under which a subdirectory will be taken or created and
     *            subsequently locked via {@link PathLocker#tryLockDirectory(Path)}
     * @param srcVersion
     * @return a {@link PathLock} whose holder is guaranteed to have an exclusive access to {@link PathLock#getPath()}
     * @throws BuildException
     *             when no such {@code i} between {@code 0} and {@link #CONCURRENCY_THRESHOLD} could be found that a
     *             directory <code>"${rootDirectory}/${projectBuildHome}/${i}"</code> could be locked.
     * @throws IOException
     */
    public PathLock openBuildDirectory(Path projectBuildHome, SrcVersion srcVersion) throws BuildException, IOException {

        Path scmRepositoryDir = rootDirectory.resolve(projectBuildHome);
        SrcdepsCoreUtils.ensureDirectoryExists(scmRepositoryDir);

        Throwable lastException = null;
        for (int i = 0; i < CONCURRENCY_THRESHOLD; i++) {
            Path checkoutDirectoryPath = scmRepositoryDir.resolve(String.valueOf(i));
            try {
                return pathLocker.lockDirectory(checkoutDirectoryPath, srcVersion);
            } catch (CannotAcquireLockException e) {
                /* nevermind, another i will work */
                lastException = e;
                log.debug("Could not get PathLock for path {}", checkoutDirectoryPath);
            }
        }

        throw new BuildException(String.format("Could not get PathLock for any of 0-%d subpaths of [%s]",
                CONCURRENCY_THRESHOLD - 1, scmRepositoryDir), lastException);

    }
}
