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
 * Note that {@link #err} can be {@code null} and that a {@code null} {@link #err} means that stdErr should be merged
 * with stdOut.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class IoRedirects {

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

    private final Redirect err;

    private final Redirect in;

    private final Redirect out;

    public IoRedirects(Redirect in, Redirect out, Redirect err) {
        super();
        SrcdepsCoreUtils.assertArgNotNull(in, "in");
        SrcdepsCoreUtils.assertArgNotNull(out, "out");
        // redirectError may be null see #isStdErrMergedWithStdOut()
        this.in = in;
        this.out = out;
        this.err = err;
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
        if (err == null) {
            if (other.err != null)
                return false;
        } else if (!err.equals(other.err))
            return false;
        if (in == null) {
            if (other.in != null)
                return false;
        } else if (!in.equals(other.in))
            return false;
        if (out == null) {
            if (other.out != null)
                return false;
        } else if (!out.equals(other.out))
            return false;
        return true;
    }

    /**
     * @return the {@link Redirect} to use for stdErr. Check {@link #isErr2Out()} before calling this method.
     * @throws IllegalStateException
     *             if {@link #isErr2Out()} returns {@code true}.
     */
    public Redirect getErr() {
        if (err == null) {
            throw new IllegalStateException("The error redirect was set to null in this " + IoRedirects.class.getName()
                    + " which means that stdErr should be merged with stdOut. Please check "
                    + IoRedirects.class.getSimpleName() + ".isStdErrMergedWithStdOut() before calling "
                    + IoRedirects.class.getSimpleName() + ".getError()");
        }
        return err;
    }

    /**
     * @return the {@link Redirect} to use for stdIn
     */
    public Redirect getIn() {
        return in;
    }

    /**
     * @return the {@link Redirect} to use for stdOut
     */
    public Redirect getOut() {
        return out;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((err == null) ? 0 : err.hashCode());
        result = prime * result + ((in == null) ? 0 : in.hashCode());
        result = prime * result + ((out == null) ? 0 : out.hashCode());
        return result;
    }

    /**
     * @return {@code true} if stdErr sould be merged with stdOut (this is the case when this {@link IoRedirects} was
     *         created with a {@code null} {@link #err}); {@code false} otherwise.
     */
    public boolean isErr2Out() {
        return err == null;
    }

}
