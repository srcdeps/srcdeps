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

import java.io.File;
import java.lang.ProcessBuilder.Redirect;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class RedirectsTest {
    private static final File file1 = new File("file1").getAbsoluteFile();
    private static final File file2 = new File("file2").getAbsoluteFile();
    private static final File file3 = new File("file3").getAbsoluteFile();

    @Test
    public void parseFrom() {
        Assert.assertEquals(Redirect.from(file1), IoRedirects.parseUri("read:" + file1.getPath()));
        Assert.assertEquals(Redirect.from(file1), IoRedirects.parseUri("READ:" + file1.getPath()));
        Assert.assertEquals(Redirect.from(file1), IoRedirects.parseUri("rEAD:" + file1.getPath()));
    }

    @Test
    public void parseTo() {
        Assert.assertEquals(Redirect.to(file1), IoRedirects.parseUri("write:" + file1.getPath()));
    }

    @Test
    public void parseAppend() {
        Assert.assertEquals(Redirect.appendTo(file1), IoRedirects.parseUri("append:" + file1.getPath()));
    }

    @Test
    public void parseErr2out() {
        Assert.assertNull(IoRedirects.parseUri("err2out"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseErr2outWithPath() {
        IoRedirects.parseUri("err2out:" + file1.getPath());
    }

    @Test
    public void parseInherit() {
        Assert.assertEquals(Redirect.INHERIT, IoRedirects.parseUri("inherit"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseInheritWithPath() {
        IoRedirects.parseUri("inherit:" + file1.getPath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseUnsupportedScheme() {
        IoRedirects.parseUri("foo:" + file1.getPath());
    }

    @Test
    public void equals() {
        Assert.assertEquals(IoRedirects.inheritAll(), IoRedirects.inheritAll());
        Assert.assertEquals(new IoRedirects(Redirect.from(file1), Redirect.to(file2), Redirect.to(file3)), new IoRedirects(Redirect.from(file1), Redirect.to(file2), Redirect.to(file3)));
    }

    @Test
    public void err2out() {
        IoRedirects rs = new IoRedirects(Redirect.from(file1), Redirect.to(file2), null);
        Assert.assertTrue(rs.isErr2Out());
        IoRedirects rs2 = new IoRedirects(Redirect.from(file1), Redirect.to(file2), Redirect.to(file3));
        Assert.assertFalse(rs2.isErr2Out());
    }
}
