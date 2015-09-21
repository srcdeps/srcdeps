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
package org.l2x6.maven.srcdeps.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;
import org.l2x6.maven.srcdeps.util.Mapper;
import org.l2x6.maven.srcdeps.util.Optional;
import org.l2x6.maven.srcdeps.util.PropsEvaluator;
import org.l2x6.maven.srcdeps.util.Supplier;

public class Repository {
    private static Collection<String> findUrls(String repoId, String defaultUrl, PropsEvaluator evaluator,
            Logger logger) {
        Collection<String> result = new LinkedHashSet<String>();
        result.add(defaultUrl);
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            String urlProp = Element.url.toSrcDepsProperty() + "." + i + "." + repoId;
            Optional<String> opt = evaluator.stringOptional("${" + urlProp + "}");
            if (opt.isPresent()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("srcdeps-maven-plugin adding URL [" + urlProp + "] : " + opt.value());
                }
                result.add(opt.value());
            } else {
                break;
            }
        }
        return result;
    }

    public static Repository load(Xpp3Dom repoElem, boolean skipTestsDefault, boolean mavenTestSkipDefault,
            PropsEvaluator evaluator, Logger logger) {
        List<String> selectors = new ArrayList<String>();
        Xpp3Dom selectorsElem = repoElem.getChild(Element.selectors.name());
        if (selectorsElem != null) {
            Xpp3Dom[] selectorElems = selectorsElem.getChildren(Element.selector.name());
            for (Xpp3Dom selectorElem : selectorElems) {
                selectors.add(selectorElem.getValue());
            }
        }

        final boolean skipTests = Optional.ofNullable(repoElem.getChild(Element.skipTests.name()))
                .map(Mapper.NODE_VALUE).map(Mapper.TO_BOOLEAN)
                .orElseGet(new Supplier.Constant<Boolean>(Boolean.valueOf(skipTestsDefault))).value();
        final boolean mavenTestSkip = Optional.ofNullable(repoElem.getChild(Element.mavenTestSkip.name()))
                .map(Mapper.NODE_VALUE).map(Mapper.TO_BOOLEAN)
                .orElseGet(new Supplier.Constant<Boolean>(Boolean.valueOf(mavenTestSkipDefault))).value();

        String repoId = repoElem.getChild(Element.id.name()).getValue();
        Collection<String> urls = findUrls(repoId, repoElem.getChild(Element.url.name()).getValue(), evaluator, logger);
        return new Repository(repoId, selectors, Collections.unmodifiableCollection(urls), skipTests, mavenTestSkip);
    }

    private final String id;
    private final boolean mavenTestSkip;
    private final List<String> selectors;
    private final boolean skipTests;
    private final Collection<String> urls;

    private Repository(String id, List<String> selectors, Collection<String> urls, boolean skipTests,
            boolean mavenTestSkip) {
        super();
        this.id = id;
        this.selectors = selectors;
        this.urls = urls;
        this.skipTests = skipTests;
        this.mavenTestSkip = mavenTestSkip;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Repository other = (Repository) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (mavenTestSkip != other.mavenTestSkip)
            return false;
        if (selectors == null) {
            if (other.selectors != null)
                return false;
        } else if (!selectors.equals(other.selectors))
            return false;
        if (skipTests != other.skipTests)
            return false;
        if (urls == null) {
            if (other.urls != null)
                return false;
        } else if (!urls.equals(other.urls))
            return false;
        return true;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (mavenTestSkip ? 1231 : 1237);
        result = prime * result + ((selectors == null) ? 0 : selectors.hashCode());
        result = prime * result + (skipTests ? 1231 : 1237);
        result = prime * result + ((urls == null) ? 0 : urls.hashCode());
        return result;
    }

    public boolean isMavenTestSkip() {
        return mavenTestSkip;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "Repository [id=" + id + ", mavenTestSkip=" + mavenTestSkip + ", selectors=" + selectors + ", skipTests="
                + skipTests + ", urls=" + urls + "]";
    }
}
