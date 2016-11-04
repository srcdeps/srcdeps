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

import java.nio.file.Path;

import javax.inject.Named;
import javax.inject.Singleton;

import org.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * The Maven builder.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@Named
@Singleton
public class MvnBuilder extends AbstractMvnBuilder {

    public static String getOsSpecificExecutable() {
        return SrcdepsCoreUtils.isWindows() ? "mvn.cmd" : "mvn";
    }

    public MvnBuilder() {
        super(getOsSpecificExecutable());
    }

    @Override
    public boolean canBuild(Path projectRootDirectory) {
        return hasPomFile(projectRootDirectory) && !hasMvnwFile(projectRootDirectory);
    }

}
