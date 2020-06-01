/*
 *  Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.usagestatistics.events;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.usagestatistics.UsageStatisticsUtils.encryptParameterValue;

/**
 * Sets parameters used to send a {@link org.hippoecm.frontend.usagestatistics.UsageEvent}.
 */
public class DocumentUsageEvent extends NodeUsageEvent {

    public static final Logger log = LoggerFactory.getLogger(DocumentUsageEvent.class);

    private static final String EVENT_PARAM_CONTEXT = "context";
    private static final String EVENT_PARAM_NAMESPACE = "namespace";
    private static final String EVENT_PARAM_TYPE = "type";

    public DocumentUsageEvent(final String name, final IModel<Node> documentModel, final String context) {
        super(name, documentModel, new HandleIdentifierStrategy());

        setParameter(EVENT_PARAM_CONTEXT, context);

        Node document = documentModel.getObject();
        if (document != null) {
            try {
                final String primaryType = document.getPrimaryNodeType().getName();
                int indexOfColon = primaryType.indexOf(':');
                if (indexOfColon > -1) {
                    setParameter(EVENT_PARAM_NAMESPACE, encryptParameterValue(primaryType.substring(0, indexOfColon)));
                    setParameter(EVENT_PARAM_TYPE, encryptParameterValue(primaryType.substring(indexOfColon + 1)));
                } else {
                    setParameter(EVENT_PARAM_TYPE, encryptParameterValue(primaryType));
                }
            } catch (RepositoryException e) {
                log.warn("Error retrieving primary type", e);
            }
        }
    }
}
