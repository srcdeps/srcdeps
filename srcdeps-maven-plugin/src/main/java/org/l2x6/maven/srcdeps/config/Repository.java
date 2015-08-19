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
import java.util.List;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.l2x6.maven.srcdeps.config.SrcdepsConfiguration.Element;

public class Repository {
    public static Repository load(Xpp3Dom repoElem) {
        List<String> selectors = new ArrayList<String>();
        Xpp3Dom selectorsElem = repoElem.getChild(Element.selectors.name());
        if (selectorsElem != null) {
            Xpp3Dom[] selectorElems = selectorsElem.getChildren(Element.selector.name());
            for (Xpp3Dom selectorElem : selectorElems) {
                selectors.add(selectorElem.getValue());
            }
        }
        return new Repository(repoElem.getChild(Element.id.name()).getValue(), selectors,
                repoElem.getChild(Element.url.name()).getValue());
    }

    private final String id;
    private final List<String> selectors;
    private final String url;

    public Repository(String id, List<String> selectors, String url) {
        super();
        this.id = id;
        this.selectors = selectors;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public List<String> getSelectors() {
        return selectors;
    }

    public String getUrl() {
        return url;
    }
}
