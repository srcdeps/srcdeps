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
package org.l2x6.srcdeps.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.l2x6.srcdeps.core.config.Configuration;
import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BuildServiceTest extends InjectedTest {
    private static final Logger log = LoggerFactory.getLogger(BuildServiceTest.class);
    private static final Path mvnLocalRepo;
    private static final String mrmSettingsXmlPath = System.getProperty("mrm.settings.xml");
    private static final Path projectsDirectory;
    private static final Path targetDirectory = Paths.get(System.getProperty("project.build.directory", "target"))
            .toAbsolutePath();

    static {
        projectsDirectory = targetDirectory.resolve("test-projects");
        mvnLocalRepo = targetDirectory.resolve("mvn-local-repo");
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    protected String currentTestName;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {
            BuildServiceTest.this.currentTestName = description.getMethodName();
        }

    };

    @BeforeClass
    public static void beforeClass() throws IOException {

        SrcdepsCoreUtils.ensureDirectoryExistsAndEmpty(mvnLocalRepo);

        System.setProperty(Configuration.SRCDEPS_MVN_SETTINGS_PROP, mrmSettingsXmlPath);

    }

    @Inject
    private BuildService buildService;

    public void assertBuild(String srcVersion) throws IOException, BuildException {
        Assert.assertNotNull("buildService not injected", buildService);
        log.info("Using {} as {}", buildService.getClass().getName(), BuildService.class.getName());

        final Path projectRoot = projectsDirectory.resolve(currentTestName);
        final Path projectBuildDirectory = projectRoot.resolve("build");

        SrcdepsCoreUtils.deleteDirectory(projectRoot);
        final String artifactDir = "org/l2x6/maven/srcdeps/itest/srcdeps-test-artifact";
        SrcdepsCoreUtils.deleteDirectory(mvnLocalRepo.resolve(artifactDir));

        BuildRequest request = BuildRequest.builder() //
                .scmUrl("git:https://github.com/l2x6/srcdeps-test-artifact.git")
                .srcVersion(SrcVersion.parse(srcVersion)).projectRootDirectory(projectBuildDirectory) //
                .buildArgument("-Dmaven.repo.local=" + mvnLocalRepo.toString()) //
                .build();

        buildService.build(request);

        final String artifactPrefix = artifactDir + "/" + srcVersion + "/srcdeps-test-artifact-" + srcVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));
    }

    @Test
    public void testMvnGitBranch() throws BuildException, IOException {
        assertBuild("0.0.1-SRC-branch-morning-branch");
    }

    @Test
    public void testMvnGitRevision() throws BuildException, IOException {
        assertBuild("0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b");
    }

    @Test
    public void testMvnGitRevisionNonMaster() throws BuildException, IOException {
        assertBuild("0.0.1-SRC-revision-dbad2cdc30b5bb3ff62fc89f57987689a5f3c220");
    }

    @Test
    public void testMvnGitTag() throws BuildException, IOException {
        assertBuild("0.0.1-SRC-tag-0.0.1");
    }

}
