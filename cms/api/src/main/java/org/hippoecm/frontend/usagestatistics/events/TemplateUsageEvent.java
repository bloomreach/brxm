/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import static org.hippoecm.frontend.usagestatistics.UsageStatisticsUtils.encryptParameterValue;

public class TemplateUsageEvent extends NodeUsageEvent {

    private static final String EVENT_PARAM_CONTEXT = "context";
    private static final String EVENT_PARAM_NAMESPACE = "namespace";
    private static final String EVENT_PARAM_TEMPLATE = "type";

    public TemplateUsageEvent(final String name, final IModel<Node> model) {
        super(name, model);

        setParameter(EVENT_PARAM_CONTEXT, "template-editor");

        Node node = model.getObject();
        if (node != null) {
            try {
                final String namespace = node.getParent().getName();
                setParameter(EVENT_PARAM_NAMESPACE, encryptParameterValue(namespace));
            } catch (RepositoryException e) {
                log.warn("Error retrieving parent node name", e);
            }

            try {
                final String template = node.getName();
                setParameter(EVENT_PARAM_TEMPLATE, encryptParameterValue(template));
            } catch (RepositoryException e) {
                log.warn("Error retrieving node name", e);
            }
        }
    }
}
