/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.document.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationStateUtils {

    private static final Logger log = LoggerFactory.getLogger(PublicationStateUtils.class);

    private PublicationStateUtils() {
    }

    /**
     * Returns the publication state of a document.
     *
     * @param variant a variant node of the document (i.e. a child node of the document's handle node)
     *
     * @return the document's publication state
     */
    public static PublicationState getPublicationState(final Node variant) {
        try {
            if (variant.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
                // hippostd:stateSummary is a mandatory property of the mixin hippostd:publishableSummary
                final String stateSummary = variant.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString();
                return PublicationState.valueOf(stateSummary.toUpperCase());
            }
        } catch (IllegalArgumentException | RepositoryException e) {
            log.warn("Could not determine publication state of document variant '{}', assuming 'unknown'", JcrUtils.getNodePathQuietly(variant), e);
        }
        return PublicationState.UNKNOWN;
    }
}
