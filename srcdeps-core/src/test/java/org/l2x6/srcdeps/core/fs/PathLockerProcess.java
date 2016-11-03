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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.l2x6.srcdeps.core.SrcVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A standalone Java application that just locks a given directory so that {@link PathLockerTest#anotherProcess()} can
 * make sure that its {@link PathLocker} running in another OS process cannot lock the same directory at the same time.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class PathLockerProcess {
    private static final Logger log = LoggerFactory.getLogger(PathLockerProcess.class);
    private static final String pid = ManagementFactory.getRuntimeMXBean().getName();

    public static void main(String[] args) {
        try {
            int i = 0;
            new PathLockerProcess(Paths.get(args[i++]), SrcVersion.parse(args[i++]), Paths.get(args[i++]), Paths.get(args[i++])).run();
        } catch (Throwable e) {
            e.printStackTrace();
            log.info(pid + " PathLockerProcess exiting");
            System.exit(500);
        }
    }

    private final Path keepRunnigFile;
    private final Path lockSuccessFile;
    private final Path lockedDirectory;
    private final SrcVersion srcVersion;

    public PathLockerProcess(Path lockedDirectory, SrcVersion srcVersion, Path keepRunnigFile, Path lockSuccessFile) {
        super();
        this.lockedDirectory = lockedDirectory;
        this.keepRunnigFile = keepRunnigFile;
        this.lockSuccessFile = lockSuccessFile;
        this.srcVersion = srcVersion;
        if (Files.notExists(keepRunnigFile)) {
            throw new IllegalStateException(String
                    .format("The keepRunnigFile [%s] should exist when PathLockerProcess is created", keepRunnigFile));
        }
        if (Files.exists(lockSuccessFile)) {
            throw new IllegalStateException(String.format(
                    "The lockSuccessFile [%s] should not exist when PathLockerProcess is created", lockSuccessFile));
        }
    }

    private void run() throws InterruptedException, IOException, CannotAcquireLockException {

        final PathLocker pathLocker = new PathLocker();

        try (PathLock lock1 = pathLocker.lockDirectory(lockedDirectory, srcVersion)) {
            /* locked for the current thread */
            log.debug(pid + " Announcing lock success in {}", lockSuccessFile);
            Files.write(lockSuccessFile, "PathLockerProcess locked this file".getBytes("utf-8"));

            while (Files.exists(keepRunnigFile)) {
                log.debug(pid + " PathLockerProcess still alive");
                Thread.sleep(10);
            }
        }
        log.debug(pid + " PathLockerProcess exiting");

    }
}
