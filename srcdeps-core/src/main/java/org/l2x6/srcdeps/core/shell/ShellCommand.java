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
package org.l2x6.srcdeps.core.shell;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A definition of a shell command that can be executed by {@link Shell#execute(ShellCommand)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ShellCommand {

    private final List<String> arguments;
    private final Map<String, String> environment;
    private final String executable;
    private final IoRedirects ioRedirects;
    private final long timeoutMs;
    private final Path workingDirectory;

    public ShellCommand(String executable, List<String> arguments, Path workingDirectory,
            Map<String, String> environment, IoRedirects ioRedirects, long timeoutMs) {
        super();
        SrcdepsCoreUtils.assertArgNotNull(executable, "executable");
        SrcdepsCoreUtils.assertArgNotNull(arguments, "arguments");
        SrcdepsCoreUtils.assertArgNotNull(workingDirectory, "workingDirectory");
        SrcdepsCoreUtils.assertArgNotNull(environment, "environment");
        SrcdepsCoreUtils.assertArgNotNull(ioRedirects, "ioRedirects");

        this.executable = executable;
        this.arguments = arguments;
        this.workingDirectory = workingDirectory;
        this.environment = environment;
        this.ioRedirects = ioRedirects;

        this.timeoutMs = timeoutMs;
    }

    /**
     * @return an array containing the executable and its arguments that can be passed e.g. to
     *         {@link ProcessBuilder#command(String...)}
     */
    public String[] asCmdArray() {
        String[] result = new String[arguments.size() + 1];
        int i = 0;
        result[i++] = executable;
        for (String arg : arguments) {
            result[i++] = arg;
        }
        return result;
    }

    /**
     * @return the {@link List} arguments for the executable. Cannot be {@code null}.
     */
    public List<String> getArguments() {
        return arguments;
    }

    /**
     * @return a {@link Map} of environment variables that should be used when executing this {@link ShellCommand}.
     *         Cannot be {@code null}. Note that these are just overlay variables - when a new {@link Process} is
     *         spawned, the environment is copied from the present process and only the variables the provided by the
     *         present method are overwritten.
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @return the executable file that should be called
     */
    public String getExecutable() {
        return executable;
    }

    /**
     * @return the {@link IoRedirects} to use when the {@link Shell} spawns a new {@link Process}
     */
    public IoRedirects getIoRedirects() {
        return ioRedirects;
    }

    /**
     * @return timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @return the directory in which this {@link ShellCommand} should be executed
     */
    public Path getWorkingDirectory() {
        return workingDirectory;
    }

}
