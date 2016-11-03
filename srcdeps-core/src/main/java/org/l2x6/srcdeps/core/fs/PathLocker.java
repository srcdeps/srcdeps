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
import java.nio.channels.FileLock;
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
 * @param <M>
 *            metadata describing the path to lock
 */
@Named
@Singleton
public class PathLocker<M> {

    /**
     * A pair consisting of a {@link ReentrantLock} and a metadata instance.
     *
     * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
     *
     * @param <M>
     *            metadata describing the path to lock
     */
    private static class LockMetadataPair<M> {
        private final ReentrantLock lock;
        private volatile M metadata;

        public LockMetadataPair(ReentrantLock lock, M metadata) {
            super();
            this.lock = lock;
            this.metadata = metadata;
        }

        public ReentrantLock getLock() {
            return lock;
        }

        public M getMetadata() {
            return metadata;
        }

        public void setMetadata(M metadata) {
            this.metadata = metadata;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(PathLocker.class);

    /**
     * An internal null-safe release of all resources
     *
     * @param lockFile
     *            the {@link RandomAccessFile} to close
     * @param lockFilePath
     *            the {@link Path} of the {@code lockFile}
     * @param threadLevelLock
     *            the {@link ReentrantLock} to release
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

    /** The map from filesystem paths to {@link LockMetadataPair}s */
    private final ConcurrentHashMap<Path, LockMetadataPair<M>> locks = new ConcurrentHashMap<>();

    /**
     * Tries to acquire both thread level and OS process level exclusive lock to the given filesystem {@code path}. As
     * for blocking of the current thread during the call of the present method there are four different cases:
     *
     * <table>
     * <tr>
     * <th colspan="3">Locked by
     * <th rowspan="2">Result
     * </tr>
     * <tr>
     * <th>another thread of the current VM
     * <th>another thread of the current VM for the same metadata
     * <th>another process
     * </tr>
     *
     * </tr>
     * <tr>
     * <td>yes</td>
     * <td>yes</td>
     * <td>no</td>
     * <td>{@link #lockDirectory(Path, Object)} waits for the path to get unlocked by other threads of current VM, tries
     * to lock on the FS level and eventually succeeds</td>
     * </tr>

     * <tr>
     * <td>yes</td>
     * <td>no</td>
     * <td>-</td>
     * <td>{@link CannotAcquireLockException} thrown immediately</td>
     * </tr>
     *
     * <tr>
     * <td>no</td>
     * <td>-</td>
     * <td>yes</td>
     * <td>{@link CannotAcquireLockException} thrown immediately</td>
     * </tr>
     *
     * <tr>
     * <td>no</td>
     * <td>-</td>
     * <td>no</td>
     * <td>Locked immediately</td>
     * </tr>
     * </table>
     *
     * <p>
     * The returned {@link PathLock} should be released using its {@link Closeable#close()} method.
     *
     * @param path
     *            the {@link Path} to lock
     * @param pathMetadata
     *            a metadata associated with the given {@code path}
     * @return a {@link PathLock} whose holder is guaranteed to have an exclusive access to {@link PathLock#getPath()}
     * @throws IOException
     *             if the given {@code path} cannot be created as a directory
     * @throws CannotAcquireLockException
     *             if the lock cannot be acquired immediately
     */
    public PathLock lockDirectory(Path path, M pathMetadata) throws IOException, CannotAcquireLockException {
        SrcdepsCoreUtils.ensureDirectoryExists(path);
        // resolve(String.valueOf(i) + ".lock");
        final LockMetadataPair<M> newPair = new LockMetadataPair<M>(new ReentrantLock(), pathMetadata);
        final LockMetadataPair<M> oldPair = locks.putIfAbsent(path, newPair);
        final LockMetadataPair<M> mdPair = oldPair == null ? newPair : oldPair;

        synchronized (mdPair) {
            final ReentrantLock lock = mdPair.getLock();
            final M oldMd = mdPair.getMetadata();
            if (oldMd.equals(pathMetadata)) {
                lock.lock();
                log.debug("Locked on thread level {}", path);
                return lockInFilesystem(path, lock);
            } else {
                /*
                 * in case the mdPair has a different metadata from a previous call we try to lock immediately and
                 * change the md only if we succeed
                 */
                if (lock.tryLock()) {
                    log.debug("Locked on thread level {}", path);
                    mdPair.setMetadata(pathMetadata);
                    return lockInFilesystem(path, lock);
                } else {
                    throw new CannotAcquireLockException(
                            String.format("Path [%s] is locked by another thread for [%s]", path, oldPair));
                }
            }
        }
    }

    private PathLock lockInFilesystem(Path path, final ReentrantLock lock) throws CannotAcquireLockException {
        Path lockFilePath = path.resolveSibling(path.getName(path.getNameCount() - 1) + ".lock");
        RandomAccessFile lockFile = null;
        try {
            lockFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
            FileLock fsLock = lockFile.getChannel().tryLock();
            log.debug("Locked on FS {} with lock {}", path, fsLock);
            if (fsLock == null) {
                throw new CannotAcquireLockException(
                        String.format("Could not acquire filesystem level lock on [%s]", lockFilePath));
            } else {
                return new PathLock(path, lockFile, lockFilePath, lock);
            }
        } catch (CannotAcquireLockException e) {
            close(lockFile, lockFilePath, lock);
            throw e;
        } catch (OverlappingFileLockException e) {
            /*
             * OverlappingFileLockException may happen if another OS level process holds the channel lock - that is a
             * normal situation, no need to log anything
             */
            close(lockFile, lockFilePath, lock);
            throw new CannotAcquireLockException(
                    String.format("Could not acquire filesystem level lock on [%s]", lockFilePath), e);
        } catch (Throwable e) {
            /* All other Exceptions are rather unexpected - log those */
            log.warn(String.format("Could not acquire a lock for path [%s]", lockFilePath), e);
            close(lockFile, lockFilePath, lock);
            throw new CannotAcquireLockException(
                    String.format("Could not acquire filesystem level lock on [%s]", lockFilePath), e);
        }
    }

}
