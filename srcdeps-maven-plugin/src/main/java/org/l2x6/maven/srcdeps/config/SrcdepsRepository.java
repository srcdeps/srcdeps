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
package org.l2x6.maven.srcdeps.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;
import org.l2x6.maven.srcdeps.util.Optional;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;

public class SrcdepsRepository {
    private static final String SCM_PREFIX = "scm:";

    private static Collection<String> findUrls(String repoId, String defaultUrl, PropsEvaluator evaluator,
            Logger logger) {
        Collection<String> result = new LinkedHashSet<String>();
        result.add(stripScm(defaultUrl));
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String urlProp = Element.url.toSrcDepsProperty() + "." + i + "." + repoId;
            Optional<String> opt = evaluator.stringOptional("${" + urlProp + "}");
            if (opt.isPresent()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("srcdeps-maven-plugin adding URL [" + urlProp + "] : " + opt.value());
                }
                result.add(stripScm(opt.value()));
            } else {
                break;
            }
        }
        return result;
    }

    public static String stripScm(String url) {
        if (url.startsWith(SCM_PREFIX)) {
            return url.substring(SCM_PREFIX.length());
        } else {
            return url;
        }
    }

    public static SrcdepsRepository load(Xpp3Dom repoElem, boolean skipTestsDefault, boolean mavenTestSkipDefault,
            PropsEvaluator evaluator, Logger logger) {
        List<String> selectors = new ArrayList<String>();
        Xpp3Dom selectorsElem = repoElem.getChild(Element.selectors.name());
        if (selectorsElem != null) {
            Xpp3Dom[] selectorElems = selectorsElem.getChildren(Element.selector.name());
            for (Xpp3Dom selectorElem : selectorElems) {
                selectors.add(selectorElem.getValue());
            }
        }

        List<String> buildArgs = new ArrayList<String>();
        Xpp3Dom buildArgsElem = repoElem.getChild(Element.buildArgs.name());
        if (buildArgsElem != null) {
            Xpp3Dom[] buildArgElems = buildArgsElem.getChildren();
            for (Xpp3Dom buildArgElem : buildArgElems) {
                buildArgs.add(buildArgElem.getValue());
            }
        }

        String repoId = repoElem.getChild(Element.id.name()).getValue();
        Collection<String> urls = findUrls(repoId, repoElem.getChild(Element.url.name()).getValue(), evaluator, logger);
        return new SrcdepsRepository(repoId, selectors, Collections.unmodifiableCollection(urls),
                Collections.unmodifiableList(buildArgs));
    }

    private final List<String> buildArguments;
    private final String id;
    private final List<String> selectors;
    private final Collection<String> urls;

    private SrcdepsRepository(String id, List<String> selectors, Collection<String> urls, List<String> buildArgs) {
        super();
        this.id = id;
        this.selectors = selectors;
        this.urls = urls;
        this.buildArguments = buildArgs;
    }

    public List<String> getBuildArguments() {
        return buildArguments;
    }

    public String getDefaultUrl() {
        return urls.iterator().next();
    }

    public String getId() {
        return id;
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public Collection<String> getUrls() {
        return urls;
    }

}
