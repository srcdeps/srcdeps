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
package org.l2x6.srcdeps.localrepo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.takari.maven.testing.TestResources;
import io.takari.maven.testing.executor.MavenExecution;
import io.takari.maven.testing.executor.MavenExecutionResult;
import io.takari.maven.testing.executor.MavenRuntime;
import io.takari.maven.testing.executor.MavenRuntime.MavenRuntimeBuilder;
import io.takari.maven.testing.executor.MavenVersions;
import io.takari.maven.testing.executor.junit.MavenJUnitTestRunner;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(MavenJUnitTestRunner.class)
@MavenVersions({ "3.3.1" })
public class SrcdepsLocalRepositoryManagerTest {
    private static final Logger log = LoggerFactory.getLogger(SrcdepsLocalRepositoryManagerTest.class);

    private static final Path mvnLocalRepo;
    private static final String mrmSettingsXmlPath = System.getProperty("mrm.settings.xml");
    private static final String projectVersion = System.getProperty("project.version");
    private static final String encoding = System.getProperty("project.build.sourceEncoding");
    private static final Path basedir = Paths.get(System.getProperty("basedir", new File("").getAbsolutePath()));
    private static final String replacementStart = "<!-- @srcdeps-maven-local-repository:version@ replacement start -->";
    private static final String replacementEnd = "<!-- @srcdeps-maven-local-repository:version@ replacement end -->";
    private static final Path srcdepsCorePath;

    static {
        srcdepsCorePath = basedir.resolve("../srcdeps-core").normalize();
        mvnLocalRepo = srcdepsCorePath.resolve("target/mvn-local-repo");
    }

    public final MavenRuntime verifier;

    @Rule
    public final TestResources resources = new TestResources() {

        @Override
        public File getBasedir(String project) throws IOException {

            Assert.assertTrue("["+ srcdepsCorePath +"] should exist", Files.exists(srcdepsCorePath));

            File result = super.getBasedir(project);

            Path extensionsXmlPath = result.toPath().resolve(".mvn/extensions.xml");

            String extensionsXmlContent = new String(Files.readAllBytes(extensionsXmlPath), encoding);

            int start = extensionsXmlContent.indexOf(replacementStart);
            Assert.assertTrue(replacementStart + " not found in "+ extensionsXmlPath, start >= 0);
            int end = extensionsXmlContent.indexOf(replacementEnd) + replacementEnd.length();
            Assert.assertTrue(replacementEnd + " not found in "+ extensionsXmlPath, end >= 0);

            String newContent = extensionsXmlContent.substring(0, start) + "<version>" + projectVersion + "</version>"
                    + extensionsXmlContent.substring(end);

            Assert.assertNotEquals(newContent, extensionsXmlContent);

            Files.write(extensionsXmlPath, newContent.getBytes(encoding));

            return result;
        }

    };
    protected String currentTestName;

    @Rule
    public TestRule watcher = new TestWatcher() {

        @Override
        protected void starting(Description description) {

            /* MavenJUnitTestRunner appends the Maven version in square brackes to the method name
             * as seen here in the test class. We want to remove the [version] suffix here, because the test projects
             * in src/test/projects are named like that */
            final String rawMethodName = description.getMethodName();
            final int bracePos = rawMethodName.indexOf('[');
            SrcdepsLocalRepositoryManagerTest.this.currentTestName = bracePos >= 0
                    ? rawMethodName.substring(0, bracePos) : rawMethodName;
        }

    };

    @BeforeClass
    public static void beforeClass() {
        Assert.assertTrue("[" + mrmSettingsXmlPath + "] should exist", Files.exists(Paths.get(mrmSettingsXmlPath)));
        Assert.assertNotNull("project.build.sourceEncoding property must be set", encoding);
        Assert.assertNotNull("project.version property must be set", projectVersion);
    }

    public SrcdepsLocalRepositoryManagerTest(MavenRuntimeBuilder runtimeBuilder) throws Exception {
        this.verifier = runtimeBuilder.withExtension(new File("target/classes").getCanonicalFile()).build();
    }

    public MavenExecutionResult assertBuild(String artifactId, String srcVersion, String goal) throws Exception {
        final String artifactDir = "org/l2x6/maven/srcdeps/itest/" + artifactId;
        SrcdepsCoreUtils.deleteDirectory(mvnLocalRepo.resolve(artifactDir));

        MavenExecution execution = verifier.forProject(resources.getBasedir(currentTestName)) //
                // .withCliOption("-X") //
                .withCliOptions("-Dmaven.repo.local=" + mvnLocalRepo.toAbsolutePath().toString()).withCliOption("-s")
                .withCliOption(mrmSettingsXmlPath);
        MavenExecutionResult result = execution.execute("clean", "compile");
        result //
                .assertErrorFreeLog() //
                .assertLogText(
                        "SrcdepsLocalRepositoryManager will decorate org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory with priority 10.0") //
        ;

        final String artifactPrefix = artifactDir + "/" + srcVersion + "/" + artifactId + "-" + srcVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));

        return result;
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    @Test
    public void mvnGitBranch() throws Exception {
        assertBuild("srcdeps-test-artifact", "0.0.1-SRC-branch-morning-branch", "compile");
    }

    @Test
    public void mvnGitInterdepModules() throws Exception {
        assertBuild("srcdeps-test-artifact-service", "0.0.1-SRC-revision-56576301d21c53439bcb5c48502c723282633cc7",
                "verify");
    }

    // @Test @Ignore // FIXME figure out why this fails
    public void mvnGitProfileAndProperties() throws Exception {
        MavenExecutionResult result = assertBuild("srcdeps-test-artifact-api",
                "0.0.1-SRC-revision-c60e73b94feac56501784be72e0081a37c8c01e9", "compile");
        result.assertLogText("[echo] Hello [random name KMYTJDb9]!");
    }

    @Test
    public void mvnGitRevision() throws Exception {
        assertBuild("srcdeps-test-artifact", "0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b", "compile");
    }

    @Test
    public void mvnGitRevisionNonMaster() throws Exception {
        assertBuild("srcdeps-test-artifact", "0.0.1-SRC-revision-dbad2cdc30b5bb3ff62fc89f57987689a5f3c220", "compile");
    }

    @Test
    public void mvnGitTag() throws Exception {
        assertBuild("srcdeps-test-artifact", "0.0.1-SRC-tag-0.0.1", "compile");
    }

}
