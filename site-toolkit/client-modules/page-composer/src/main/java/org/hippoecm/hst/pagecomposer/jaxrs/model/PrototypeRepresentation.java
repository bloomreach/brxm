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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrototypeRepresentation extends ComponentRepresentation {

    private static final Logger log = LoggerFactory.getLogger(PrototypeRepresentation.class);

    private String displayName;
    private String primaryContainer;

    public PrototypeRepresentation represent(HstComponentConfiguration componentConfiguration, PageComposerContextService pageComposerContextService) {
        super.represent(componentConfiguration, pageComposerContextService.getEditingMount());

        // if the prototype contains meta data, we fetch it separately because it is not part of the HstComponentConfiguration
        try {
            final Session session = pageComposerContextService.getRequestContext().getSession();
            Node prototypeNode  = session.getNodeByIdentifier(componentConfiguration.getCanonicalIdentifier());
            displayName = JcrUtils.getStringProperty(prototypeNode,
                    HstNodeTypes.PROTOTYPE_META_PROPERTY_DISPLAY_NAME, getName());
            primaryContainer = JcrUtils.getStringProperty(prototypeNode,
                    HstNodeTypes.PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER, null);
        } catch (ItemNotFoundException e) {
            String msg = String.format("Expected to find prototype for '%s' but UUID '%s' not found",
                    componentConfiguration.getCanonicalStoredLocation(), componentConfiguration.getCanonicalIdentifier());
            throw new IllegalStateException(msg);
        }catch (RepositoryException e) {
            log.warn("RepositoryException while fetching prototype node for '{}'",
                    componentConfiguration.getCanonicalStoredLocation(), e);
            String msg = String.format("Expected to find prototype for '%s' but got a repository exception", e.toString());
            throw new IllegalStateException(msg);
        }
        return this;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getPrimaryContainer() {
        return primaryContainer;
    }

    public void setPrimaryContainer(final String primaryContainer) {
        this.primaryContainer = primaryContainer;
    }
}
