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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.l2x6.srcdeps.core.BuildException;
import org.l2x6.srcdeps.core.BuildRequest;
import org.l2x6.srcdeps.core.BuildRequest.Verbosity;
import org.l2x6.srcdeps.core.shell.Shell;
import org.l2x6.srcdeps.core.shell.ShellCommand;

/**
 * A base for {@link MvnBuilder} and {@link MvnwBuilder}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractMvnBuilder extends ShellBuilder {
    public static final List<String> mvnDefaultArgs = Collections
            .unmodifiableList(Arrays.asList("clean", "install", "-DskipTests"));

    public static final List<String> mvnwFileNames = Collections.unmodifiableList(Arrays.asList("mvnw", "mvnw.cmd"));
    public static final List<String> pomFileNames = Collections.unmodifiableList(
            Arrays.asList("pom.xml", "pom.atom", "pom.clj", "pom.groovy", "pom.rb", "pom.scala", "pom.yml"));

    public static boolean hasMvnwFile(Path directory) {
        for (String fileName : mvnwFileNames) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPomFile(Path directory) {
        for (String fileName : pomFileNames) {
            if (directory.resolve(fileName).toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    public AbstractMvnBuilder(String executable) {
        super(executable);
    }

    @Override
    protected List<String> getDefaultBuildArguments() {
        return mvnDefaultArgs;
    }

    @Override
    protected List<String> getVerbosityArguments(Verbosity verbosity) {
        switch (verbosity) {
        case trace:
        case debug:
            return Collections.singletonList("--debug");
        case info:
            return Collections.emptyList();
        case warn:
        case error:
            return Collections.singletonList("--quiet");
        default:
            throw new IllegalStateException("Unexpected " + Verbosity.class.getName() + " value [" + verbosity + "]");
        }
    }

    @Override
    public void setVersions(BuildRequest request) throws BuildException {
        final List<String> args = Arrays.asList("versions:set", "-DnewVersion=" + request.getSrcVersion().toString(),
                "-DgenerateBackupPoms=false");
        ShellCommand cliRequest = new ShellCommand(executable, args, request.getProjectRootDirectory(),
                request.getBuildEnvironment(), request.getIoRedirects(), request.getTimeoutMs());
        Shell.execute(cliRequest).assertSuccess();
    }

}
