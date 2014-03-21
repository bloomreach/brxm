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
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrototypesRepresentation {

    private static final Logger log = LoggerFactory.getLogger(PrototypesRepresentation.class);
    private List<PrototypeRepresentation> prototypes = new ArrayList<>();

    public PrototypesRepresentation represent(final HstSite editingPreviewSite,
                                         final boolean includeInherited,
                                         final PageComposerContextService pageComposerContextService) {
        for (HstComponentConfiguration page : editingPreviewSite.getComponentsConfiguration().getPrototypePages().values()) {
            if (!includeInherited && page.isInherited()) {
                // skipping inherited
                continue;
            }
            try {
                PrototypeRepresentation prototypeRepresentation = new PrototypeRepresentation().represent(page, pageComposerContextService);
                prototypes.add(prototypeRepresentation);
            } catch (IllegalArgumentException e) {
                log.warn("Skip prototype for '{}'", page.getCanonicalStoredLocation(), e);
            }
        }
        Collections.sort(prototypes, new Comparator<PrototypeRepresentation>() {
            @Override
            public int compare(final PrototypeRepresentation o1, final PrototypeRepresentation o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
        return this;
    }

    public List<PrototypeRepresentation> getPrototypes() {
        return prototypes;
    }

    public void setPrototypes(final List<PrototypeRepresentation> prototypes) {
        this.prototypes = prototypes;
    }
}
