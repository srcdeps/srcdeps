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
package org.srcdeps.core.config;

import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ScmRepositoryTest {

    @Test
    public void idValid() {
        ScmRepository.assertValidId("simple");
        ScmRepository.assertValidId("one.two.three");
    }

    @Test
    public void idAsPathValid() {
        Assert.assertEquals(Paths.get("simple"), ScmRepository.builder().id("simple").build().getIdAsPath());
        Assert.assertEquals(Paths.get("one", "two", "three"),
                ScmRepository.builder().id("one.two.three").build().getIdAsPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void idNull() {
        ScmRepository.assertValidId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void idEmpty() {
        ScmRepository.assertValidId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idSinglePeriod() {
        ScmRepository.assertValidId(".");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idStartsWithPeriod() {
        ScmRepository.assertValidId(".foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idEndsWithPeriod() {
        ScmRepository.assertValidId("foo.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idWithMultipleSubsequentPeriod() {
        ScmRepository.assertValidId("foo..bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idInvalidStart() {
        ScmRepository.assertValidId(" foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idInvalidSegmentStart() {
        ScmRepository.assertValidId("foo. bar");
    }

    @Test(expected = IllegalArgumentException.class)
    public void idInvalidSegmentPart() {
        ScmRepository.assertValidId("foo.b ar");
    }

}
