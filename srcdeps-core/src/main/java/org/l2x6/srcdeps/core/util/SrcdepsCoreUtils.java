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
package org.l2x6.srcdeps.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

/**
 * The utilities.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class SrcdepsCoreUtils {

    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

    public static void assertArgNotEmptyString(String value, String argName) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be an empty String", argName));
        }
    }

    public static void assertArgNotNull(Object value, String argName) {
        if (value == null) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be null", argName));
        }
    }

    public static void assertCollectionNotEmpty(Collection<?> value, String argName) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(String.format("Argument [%s] cannot be an empty collection", argName));
        }
    }

    /**
     * Deletes a file or directory recursivelly if it exists.
     *
     * @param directory
     *            the directory to delete
     * @throws IOException
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed; propagate exception
                        throw exc;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    // try to delete the file anyway, even if its attributes
                    // could not be read, since delete-only access is
                    // theoretically possible
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * @return {@code true} if the operating system the present JVM runs on is Windows; {@code false} otherwise
     */
    public static boolean isWindows() {
        return isWindows;
    }

    private SrcdepsCoreUtils() {
    }

}
