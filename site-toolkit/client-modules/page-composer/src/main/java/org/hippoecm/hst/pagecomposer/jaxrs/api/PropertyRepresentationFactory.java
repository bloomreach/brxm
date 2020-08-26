/*
 *  Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.util.AbstractHstComponentParameters;

public interface PropertyRepresentationFactory {

    /**
     * @param locale
     * @param contentPath
     * @param prefix
     * @param containerItemNode the container item node, either part of the hst configuration or an XPage document
     * @param componentConfiguration the HstComponentConfiguration for {@code containerItemNode} which can be an hst
     *                               configuration component item or an XPage document component
     * containerItemNode} is a container node of an XPage document
     * @param componentParameters
     * @param properties the already existing {@link ContainerItemComponentPropertyRepresentation}s
     * @return a <code>ContainerItemComponentPropertyRepresentation</code> instance or <code>null</code> if no
     * {@link ContainerItemComponentPropertyRepresentation} needs to be added (because for example already
     * representation for parameter exists)
     * @throws {@link java.lang.RuntimeException} in case some unexpected exception occurs
     */
    ContainerItemComponentPropertyRepresentation createProperty(
            final Locale locale,
            final String contentPath,
            final String prefix,
            final Node containerItemNode,
            final HstComponentConfiguration componentConfiguration,
            final AbstractHstComponentParameters componentParameters,
            final List<ContainerItemComponentPropertyRepresentation> properties);
}
