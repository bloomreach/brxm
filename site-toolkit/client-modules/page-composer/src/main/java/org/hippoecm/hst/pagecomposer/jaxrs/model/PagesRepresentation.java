/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;

public class PagesRepresentation {

    private List<PageRepresentation> pages = new ArrayList<>();

    public PagesRepresentation represent(final HstSite editingPreviewSite,
                                         final boolean prototypeOnly,
                                         final boolean includeInherited) {
        for (HstComponentConfiguration page : editingPreviewSite.getComponentsConfiguration().getPages().values()) {
            if (prototypeOnly && !page.isPrototype()) {
                // skipping non prototype
                continue;
            }
            if (!includeInherited && page.isInherited()) {
                // skipping inherited
                continue;
            }
            PageRepresentation pageRepresentation = new PageRepresentation().represent(page);
            pages.add(pageRepresentation);
        }
        Collections.sort(pages, new Comparator<PageRepresentation>() {
            @Override
            public int compare(final PageRepresentation o1, final PageRepresentation o2) {
               return o1.getName().compareTo(o2.getName());
            }
        });
        return this;
    }

    public List<PageRepresentation> getPages() {
        return pages;
    }

    public void setPages(final List<PageRepresentation> pages) {
        this.pages = pages;
    }
}
