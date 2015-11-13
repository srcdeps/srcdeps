/**
 * Copyright 2015 Maven Source Dependencies
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
package org.l2x6.maven.srcdeps.util;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.logging.Logger;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;

public class ArgsBuilder {
    private final StringBuilder builder = new StringBuilder();
    private final SrcdepsConfiguration configuration;
    private final PropsEvaluator evaluator;
    private final Logger logger;
    private final MavenSession session;

    public ArgsBuilder(SrcdepsConfiguration configuration, MavenSession session, PropsEvaluator evaluator,
            Logger logger) {
        super();
        this.configuration = configuration;
        this.evaluator = evaluator;
        this.logger = logger;
        this.session = session;
        if (configuration.isQuiet()) {
            opt("-q");
        }
    }

    public String build() {
        return builder.length() == 0 ? null : builder.toString();
    }

    public ArgsBuilder buildOptions() {
        String rawFwdProps = evaluator.stringOptional(Element.forwardProperties)
                .orElseGet(Supplier.Constant.EMPTY_STRING).value();
        property(Element.forwardProperties.toSrcDepsProperty(), rawFwdProps);

        Set<String> useProps = new LinkedHashSet<String>(configuration.getForwardProperties());
        for (String prop : configuration.getForwardProperties()) {
            if (prop.endsWith("*")) {
                /* prefix */
                String prefix = prop.substring(prop.length() - 1);
                for (Object key : session.getUserProperties().keySet()) {
                    if (key instanceof String && ((String) key).startsWith(prefix)) {
                        useProps.add((String) key);
                    }
                }
                for (Object key : session.getSystemProperties().keySet()) {
                    if (key instanceof String && ((String) key).startsWith(prefix)) {
                        useProps.add((String) key);
                    }
                }
            }
        }

        for (String prop : useProps) {
            String expression = "${" + prop + "}";
            Optional<String> value = evaluator.stringOptional(expression);
            if (value.isPresent()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("srcdeps-maven-plugin  evaluated " + expression + " as " + value.value());
                }
                property(prop, value.value());
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("srcdeps-maven-plugin  is not forwarding property " + prop
                            + " because it has no value");
                }
            }
        }

        if (configuration.isMavenTestSkip()) {
            property("maven.test.skip", String.valueOf(configuration.isMavenTestSkip()));
        }
        if (configuration.isSkipTests()) {
            property("skipTests", String.valueOf(configuration.isSkipTests()));
        }

        return this;
    }

    public ArgsBuilder nonDefaultProp(String key, boolean value, boolean defaultValue) {
        if (defaultValue != value) {
            property(key, String.valueOf(value));
        }
        return this;
    }

    public ArgsBuilder opt(String opt) {
        if (builder.length() != 0) {
            builder.append(' ');
        }
        // FIXME: we should check and eventually quote and/or escape the the
        // whole param
        builder.append(opt);
        return this;
    }

    public ArgsBuilder profiles(List<String> profiles) {
        if (profiles != null && !profiles.isEmpty()) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            // FIXME: we should check and eventually quote and/or escape the
            // the
            // whole param
            builder.append("-P");
            boolean first = true;
            for (String profile : profiles) {
                if (first) {
                    first = false;
                } else {
                    builder.append(',');
                }
                builder.append(profile);
            }
        }
        return this;
    }

    public ArgsBuilder properties(Map<String, String> props) {
        for (Map.Entry<String, String> en : props.entrySet()) {
            property(en.getKey(), en.getValue());
        }
        return this;
    }

    public ArgsBuilder property(String key, String value) {
        if (builder.length() != 0) {
            builder.append(' ');
        }
        builder.append("\"-D").append(key).append('=').append(value).append('\"');
        return this;
    }
}