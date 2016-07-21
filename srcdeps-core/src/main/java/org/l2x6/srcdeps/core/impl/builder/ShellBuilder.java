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
package org.l2x6.srcdeps.core.impl.builder;

import java.util.ArrayList;
import java.util.List;

import org.l2x6.srcdeps.core.BuildException;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.BuildRequest.Verbosity;
import org.l2x6.srcdeps.core.Builder;
import org.l2x6.srcdeps.core.shell.Shell;
import org.l2x6.srcdeps.core.shell.ShellCommand;

/**
 * A base class for command line build tools.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class ShellBuilder implements Builder {

    protected final String executable;

    /**
     * @param executable
     *            the executable such as {@code mvn}
     */
    public ShellBuilder(String executable) {
        super();
        this.executable = executable;
    }

    @Override
    public void build(BuildRequest request) throws BuildException {
        List<String> args = mergedBuildArguments(request, getDefaultBuildArguments(),
                getVerbosityArguments(request.getVerbosity()), getSkipTestsArguments(request.isSkipTests()));
        ShellCommand command = new ShellCommand(executable, args, request.getProjectRootDirectory(),
                request.getBuildEnvironment(), request.getIoRedirects(), request.getTimeoutMs());
        Shell.execute(command).assertSuccess();
    }

    public static List<String> mergedBuildArguments(BuildRequest request, List<String> defaultArguments,
            List<String> verbosityArguments, List<String> skipTestsArguments) {
        List<String> result = new ArrayList<>();
        if (request.isAddDefaultBuildArguments()) {
            result.addAll(defaultArguments);
        }
        result.addAll(request.getBuildArguments());
        result.addAll(verbosityArguments);
        result.addAll(skipTestsArguments);

        return result;
    }

    protected abstract List<String> getDefaultBuildArguments();

    protected abstract List<String> getVerbosityArguments(Verbosity verbosity);

    protected abstract List<String> getSkipTestsArguments(boolean skipTests);

}
