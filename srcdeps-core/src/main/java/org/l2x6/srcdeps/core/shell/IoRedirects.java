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
import java.util.Locale;

import org.l2x6.srcdeps.core.util.SrcdepsCoreUtils;

/**
 * A triple of {@link Redirect}s to use when creating a new {@link Process}.
 * <p>
 * Note that {@link #stderr} can be {@code null} and that a {@code null} {@link #stderr} means that stdErr should be merged
 * with stdOut.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class IoRedirects {

    public static class Builder {

        private Redirect stderr = Redirect.INHERIT;
        private Redirect stdin = Redirect.INHERIT;
        private Redirect stdout = Redirect.INHERIT;

        public IoRedirects build() {
            return new IoRedirects(stdin, stdout, stderr);
        }

        public Builder stderr(Redirect stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder stdin(Redirect stdin) {
            this.stdin = stdin;
            return this;
        }

        public Builder stdout(Redirect stdout) {
            this.stdout = stdout;
            return this;
        }
    }
    /**
     * An enum of prefixes to use when encoding {@link Redirect}s as URI {@link String}s. See
     * {@link IoRedirects#parseUri(String)}.
     *
     */
    public enum RedirectScheme {
        append, err2out, inherit, read, write
    }

    private static final IoRedirects INHERIT_ALL = new IoRedirects(Redirect.INHERIT, Redirect.INHERIT,
            Redirect.INHERIT);

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return a {@link IoRedirects} singleton with all three {@link Redirect} fields set to {@link Redirect#INHERIT}.
     */
    public static IoRedirects inheritAll() {
        return INHERIT_ALL;
    }

    /**
     * Parses the given URI into a new {@link Redirect}. The URI is supposed to start with one of {@link RedirectScheme}
     * prefixes. Examples of valid valid URIs: {@code read:/path/to/input-file.txt}, {@code write:/path/to/log.txt},
     * {@code append:/path/to/log.txt}, {@code inherit} {@code err2out}.
     *
     * @param uri
     *            the URI to parse
     * @return a new {@link Redirect}
     * @throws IllegalArgumentException
     *             if the given {@code uri} is not in proper format
     */
    public static Redirect parseUri(String uri) {
        SrcdepsCoreUtils.assertArgNotNull(uri, "uri");
        SrcdepsCoreUtils.assertArgNotEmptyString(uri, "uri");

        int pos = uri.indexOf(':');

        switch (pos) {
        case -1:
            pos = uri.length();
            break;
        case 0:
            throw new IllegalArgumentException(String.format("Colon found at position 0 of URI string [%s]", uri));
        default:
            break;
        }

        final String redirectScheme = uri.substring(0, pos).toLowerCase(Locale.US);
        final RedirectScheme scheme = RedirectScheme.valueOf(redirectScheme);

        pos++;
        final String path = pos < uri.length() ? uri.substring(pos) : null;

        switch (scheme) {
        case err2out:
            if (path != null) {
                throw new IllegalArgumentException(
                        String.format("Unexpected characters found after [err2out] in [%s]", uri));
            }
            return null;
        case read:
            return Redirect.from(new File(path));
        case write:
            return Redirect.to(new File(path));
        case append:
            return Redirect.appendTo(new File(path));
        case inherit:
            if (path != null) {
                throw new IllegalArgumentException(
                        String.format("Unexpected characters found after [inherit] in [%s]", uri));
            }
            return Redirect.INHERIT;
        default:
            throw new IllegalStateException(String.format(
                    "Unexpected redirect type [%s] in redirect URI [%s] only [read], [write], [append], [inherit] are supported. In addition, you can use [err2out] for the error stream",
                    redirectScheme, uri));
        }
    }

    private final Redirect stderr;

    private final Redirect stdin;

    private final Redirect stdout;

    public IoRedirects(Redirect in, Redirect out, Redirect err) {
        super();
        SrcdepsCoreUtils.assertArgNotNull(in, "in");
        SrcdepsCoreUtils.assertArgNotNull(out, "out");
        // redirectError may be null see #isStdErrMergedWithStdOut()
        this.stdin = in;
        this.stdout = out;
        this.stderr = err;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        IoRedirects other = (IoRedirects) obj;
        if (stderr == null) {
            if (other.stderr != null)
                return false;
        } else if (!stderr.equals(other.stderr))
            return false;
        if (stdin == null) {
            if (other.stdin != null)
                return false;
        } else if (!stdin.equals(other.stdin))
            return false;
        if (stdout == null) {
            if (other.stdout != null)
                return false;
        } else if (!stdout.equals(other.stdout))
            return false;
        return true;
    }

    /**
     * @return the {@link Redirect} to use for stdErr. Check {@link #isErr2Out()} before calling this method.
     * @throws IllegalStateException
     *             if {@link #isErr2Out()} returns {@code true}.
     */
    public Redirect getStderr() {
        if (stderr == null) {
            throw new IllegalStateException("The error redirect was set to null in this " + IoRedirects.class.getName()
                    + " which means that stdErr should be merged with stdOut. Please check "
                    + IoRedirects.class.getSimpleName() + ".isStdErrMergedWithStdOut() before calling "
                    + IoRedirects.class.getSimpleName() + ".getError()");
        }
        return stderr;
    }

    /**
     * @return the {@link Redirect} to use for stdIn
     */
    public Redirect getStdin() {
        return stdin;
    }

    /**
     * @return the {@link Redirect} to use for stdOut
     */
    public Redirect getStdout() {
        return stdout;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stderr == null) ? 0 : stderr.hashCode());
        result = prime * result + ((stdin == null) ? 0 : stdin.hashCode());
        result = prime * result + ((stdout == null) ? 0 : stdout.hashCode());
        return result;
    }

    /**
     * @return {@code true} if stdErr sould be merged with stdOut (this is the case when this {@link IoRedirects} was
     *         created with a {@code null} {@link #stderr}); {@code false} otherwise.
     */
    public boolean isErr2Out() {
        return stderr == null;
    }

}
