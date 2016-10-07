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
package org.l2x6.srcdeps.core.fs;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Named;
import javax.inject.Singleton;

import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple facility to guarantee both thread level and OS process level exclusive access to a filesystem path.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class PathLocker {
    private static final Logger log = LoggerFactory.getLogger(PathLocker.class);

    /** The map from filesystem paths to thread level locks */
    private final ConcurrentHashMap<Path, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Tries to acquire both thread level and OS process level exclusive lock to the given filesystem {@code path}.
     * Immediately throws a {@link CannotAcquireLockException} in case the given {@code path} cannot be locked rather
     * than waiting for a lock.
     * <p>
     * The returned {@link PathLock} should be released using its {@link Closeable#close()} method.
     *
     * @param path
     *            the {@link Path} to lock
     * @return a {@link PathLock} whose holder is guaranteed to have an exclusive access to {@link PathLock#getPath()}
     * @throws IOException if the given {@code path} cannot be created as a directory
     * @throws CannotAcquireLockException if the lock cannot be acquired immediately
     */
    public PathLock tryLockDirectory(Path path) throws IOException, CannotAcquireLockException {
        SrcdepsCoreUtils.ensureDirectoryExists(path);
        Path lockFilePath = path.resolveSibling(path.getName(path.getNameCount() - 1) + ".lock");
        // resolve(String.valueOf(i) + ".lock");
        final ReentrantLock newLock = new ReentrantLock();
        final ReentrantLock oldLock = locks.putIfAbsent(lockFilePath, newLock);
        final ReentrantLock threadLevelLock = oldLock == null ? newLock : oldLock;
        if (threadLevelLock.tryLock()) {
            RandomAccessFile lockFile = null;
            try {
                lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
                lockFile.getChannel().tryLock();
                return new PathLock(path, lockFile, lockFilePath, threadLevelLock);
            } catch (OverlappingFileLockException e) {
                /*
                 * OverlappingFileLockException may happen if another OS level process holds the channel lock - that is
                 * a normal situation, no need to log anything
                 */
                close(lockFile, lockFilePath, threadLevelLock);
                throw new CannotAcquireLockException(
                        String.format("Could not acquire filesystem level lock on [%s]", lockFilePath), e);
            } catch (Throwable e) {
                /* All other Exceptions are rather unexpected - log those */
                log.warn(String.format("Could not acquire a lock for path [%s]", lockFilePath), e);
                close(lockFile, lockFilePath, threadLevelLock);
                throw new CannotAcquireLockException(
                        String.format("Could not acquire filesystem level lock on [%s]", lockFilePath), e);
            }
        } else {
            throw new CannotAcquireLockException(String.format("Path [%s] is locked by another thread", lockFilePath));
        }
    }

    /**
     * An internal null-safe release of all resources
     * @param lockFile
     * @param lockFilePath
     * @param threadLevelLock
     */
    private static void close(RandomAccessFile lockFile, Path lockFilePath, ReentrantLock threadLevelLock) {
        if (lockFile != null) {
            try {
                lockFile.close();
            } catch (IOException e1) {
                log.warn(String.format("Could not close lock file [%s]", lockFilePath), e1);
            }
        }
        threadLevelLock.unlock();
    }

}
