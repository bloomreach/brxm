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
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;

public class PrototypePagesRepresentation {

    private List<ComponentRepresentation> pages = new ArrayList<>();

    public PrototypePagesRepresentation represent(final HstSite editingPreviewSite,
                                         final boolean includeInherited,
                                         final Mount mount) {
        for (HstComponentConfiguration page : editingPreviewSite.getComponentsConfiguration().getPrototypePages().values()) {
            if (!includeInherited && page.isInherited()) {
                // skipping inherited
                continue;
            }
            ComponentRepresentation pageRepresentation = new ComponentRepresentation().represent(page, mount);
            pages.add(pageRepresentation);
        }
        Collections.sort(pages, new Comparator<ComponentRepresentation>() {
            @Override
            public int compare(final ComponentRepresentation o1, final ComponentRepresentation o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return this;
    }

    public List<ComponentRepresentation> getPages() {
        return pages;
    }

    public void setPages(final List<ComponentRepresentation> pages) {
        this.pages = pages;
    }
}
