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
import java.nio.file.Path;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lock which guarantees its holder to have an exclusive access to {@link #getPath()}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class PathLock implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(PathLock.class);
    private final Path lockFilePath;
    private final Path path;
    private final RandomAccessFile lockFile;
    private final ReentrantLock threadLevelLock;

    public PathLock(Path path, RandomAccessFile lockFile, Path lockFilePath, ReentrantLock threadLevelLock) {
        this.path = path;
        this.lockFile = lockFile;
        this.lockFilePath = lockFilePath;
        this.threadLevelLock = threadLevelLock;
    }

    /**
     * @return the {@link Path} locked
     */
    public Path getPath() {
        return path;
    }

    /**
     * Releases this {@link PathLock}.
     */
    @Override
    public void close() {
        try {
            lockFile.close();
        } catch (IOException e) {
            log.warn(String.format("Could not close lock file [%s]", lockFilePath), e);
        }
        threadLevelLock.unlock();
    }

}
