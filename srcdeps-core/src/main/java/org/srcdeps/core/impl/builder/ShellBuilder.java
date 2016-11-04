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
package org.srcdeps.core.impl.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.srcdeps.core.BuildException;
import org.srcdeps.core.BuildRequest;
import org.srcdeps.core.BuildRequest.Verbosity;
import org.srcdeps.core.Builder;
import org.srcdeps.core.shell.Shell;
import org.srcdeps.core.shell.ShellCommand;

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

        List<String> args = mergeArguments(request);
        ShellCommand command = ShellCommand.builder() //
                .executable(executable) //
                .arguments(args) //
                .workingDirectory(request.getProjectRootDirectory()) //
                .environment(request.getBuildEnvironment()) //
                .ioRedirects(request.getIoRedirects()) //
                .timeoutMs(request.getTimeoutMs()) //
                .build();
        Shell.execute(command).assertSuccess();
    }

    protected abstract List<String> getDefaultBuildArguments();

    protected List<String> getForwardPropertiesArguments(Set<String> fwdPropNames) {
        List<String> result = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for (String propName : fwdPropNames) {
            if (names.length() > 0) {
                names.append(',');
            }
            names.append(propName);

            if (propName.endsWith("*")) {
                /* prefix */
                String prefix = propName.substring(propName.length() - 1);
                for (Object key : System.getProperties().keySet()) {
                    if (key instanceof String && ((String) key).startsWith(prefix)) {
                        String value = System.getProperty((String) key);
                        if (value != null) {
                            result.add("-D" + propName + "=" + value);
                        }
                    }
                }
            } else {
                String value = System.getProperty(propName);
                if (value != null) {
                    result.add("-D" + propName + "=" + value);
                }
            }
        }

        result.add("-Dsrcdeps.forwardProperties=" + names.toString());
        return result;
    }

    protected abstract List<String> getSkipTestsArguments(boolean skipTests);

    protected abstract List<String> getVerbosityArguments(Verbosity verbosity);

    protected List<String> mergeArguments(BuildRequest request) {
        List<String> result = new ArrayList<>();
        if (request.isAddDefaultBuildArguments()) {
            result.addAll(getDefaultBuildArguments());
        }
        result.addAll(request.getBuildArguments());
        result.addAll(getVerbosityArguments(request.getVerbosity()));
        result.addAll(getSkipTestsArguments(request.isSkipTests()));
        result.addAll(getForwardPropertiesArguments(request.getForwardProperties()));
        return result;
    }

}
