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
package org.srcdeps.mvn.localrepo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.srcdeps.core.util.SrcdepsCoreUtils;

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
    private static final Pattern replacementPattern = Pattern.compile(Pattern.quote("<version>") + "[^<]+" + Pattern.quote("</version><!-- @srcdeps.version@ -->"));
    private static final Path srcdepsCorePath;
    private static final Path srcdepsQuickstartsPath;

    static {
        srcdepsCorePath = basedir.resolve("../srcdeps-core").normalize();
        srcdepsQuickstartsPath = basedir.resolve("../srcdeps-maven-quickstarts").normalize();
        mvnLocalRepo = srcdepsCorePath.resolve("target/mvn-local-repo");
    }

    public final MavenRuntime verifier;

    @Rule
    public final TestResources resources = new TestResources(srcdepsQuickstartsPath.toString(), "target/test-projects") {

        @Override
        public File getBasedir(String project) throws IOException {

            Assert.assertTrue("["+ srcdepsCorePath +"] should exist", Files.exists(srcdepsCorePath));

            File result = super.getBasedir(project);

            Path extensionsXmlPath = result.toPath().resolve(".mvn/extensions.xml");

            String extensionsXmlContent = new String(Files.readAllBytes(extensionsXmlPath), encoding);

            String newContent = replacementPattern.matcher(extensionsXmlContent).replaceAll("<version>" + projectVersion + "</version>");

            Assert.assertNotEquals(newContent, extensionsXmlContent);

            Files.write(extensionsXmlPath, newContent.getBytes(encoding));

            return result;
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

    public MavenExecutionResult assertBuild(String project, String srcArtifactId, String srcVersion, String... goals) throws Exception {

        log.info("Building test project {}", project);

        String srcGroupDir = "org/l2x6/maven/srcdeps/itest";
        SrcdepsCoreUtils.deleteDirectory(mvnLocalRepo.resolve(srcGroupDir));
        final String testArtifactDir = srcGroupDir  + "/" + srcArtifactId;

        final String quickstartRepoDir = "org/l2x6/srcdeps/quickstarts/" + project;
        SrcdepsCoreUtils.deleteDirectory(mvnLocalRepo.resolve(quickstartRepoDir));

        MavenExecution execution = verifier.forProject(resources.getBasedir(project)) //
                // .withCliOption("-X") //
                .withCliOptions("-Dmaven.repo.local=" + mvnLocalRepo.toAbsolutePath().toString()).withCliOption("-s")
                .withCliOption(mrmSettingsXmlPath);
        MavenExecutionResult result = execution.execute(goals);
        result //
                .assertErrorFreeLog() //
                .assertLogText(
                        "SrcdepsLocalRepositoryManager will decorate org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory with priority 10.0") //
        ;

        final String artifactPrefix = testArtifactDir + "/" + srcVersion + "/" + srcArtifactId + "-" + srcVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));

        return result;
    }

    public static void assertExists(Path path) {
        Assert.assertTrue(String.format("File or directory does not exist [%s]", path.toString()), Files.exists(path));
    }

    @Test
    public void mvnGitBranch() throws Exception {
        String project = "srcdeps-mvn-git-branch-quickstart";
        assertBuild(project, "srcdeps-test-artifact", "0.0.1-SRC-branch-morning-branch", "clean", "install");

        final String quickstartRepoDir = "org/l2x6/srcdeps/quickstarts/" + project + "/" + project + "-jar";
        String dependentVersion = "1.0-SNAPSHOT";
        final String artifactPrefix = quickstartRepoDir + "/" + dependentVersion + "/" + project + "-jar-" + dependentVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));
    }

    @Test
    public void mvnGitInterdepModules() throws Exception {
        assertBuild("srcdeps-mvn-git-interdep-modules-quickstart", "srcdeps-test-artifact-service", "0.0.1-SRC-revision-56576301d21c53439bcb5c48502c723282633cc7",
                "clean", "verify");
    }

    @Test
    public void mvnGitProfileAndProperties() throws Exception {
        MavenExecutionResult result = assertBuild("srcdeps-mvn-git-profile-and-properties-quickstart", "srcdeps-test-artifact-api",
                "0.0.1-SRC-revision-834947e286f1f59bd6c5c3ca3823f4656bc9345b", "clean", "test");
    }

    @Test
    public void mvnGitRevision() throws Exception {
        String project = "srcdeps-mvn-git-revision-quickstart";
        assertBuild(project, "srcdeps-test-artifact", "0.0.1-SRC-revision-66ea95d890531f4eaaa5aa04a9b1c69b409dcd0b", "clean", "install");

        final String quickstartRepoDir = "org/l2x6/srcdeps/quickstarts/" + project + "/" + project + "-jar";
        String dependentVersion = "1.0-SNAPSHOT";
        final String artifactPrefix = quickstartRepoDir + "/" + dependentVersion + "/" + project + "-jar-" + dependentVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));
    }

    @Test
    public void mvnGitRevisionNonMaster() throws Exception {
        assertBuild("srcdeps-mvn-git-revision-non-master-quickstart", "srcdeps-test-artifact", "0.0.1-SRC-revision-dbad2cdc30b5bb3ff62fc89f57987689a5f3c220", "clean", "compile");
    }

    @Test
    public void mvnGitTag() throws Exception {
        String project = "srcdeps-mvn-git-tag-quickstart";
        assertBuild(project, "srcdeps-test-artifact", "0.0.1-SRC-tag-0.0.1", "clean", "install");
        final String quickstartRepoDir = "org/l2x6/srcdeps/quickstarts/" + project + "/" + project + "-jar";
        String dependentVersion = "1.0-SNAPSHOT";
        final String artifactPrefix = quickstartRepoDir + "/" + dependentVersion + "/" + project + "-jar-" + dependentVersion;
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".jar"));
        assertExists(mvnLocalRepo.resolve(artifactPrefix + ".pom"));
    }

}
