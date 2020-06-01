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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import java.util.List;
import java.util.Locale;

import javax.jcr.Node;

import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;

public interface PropertyRepresentationFactory {

    /**
     *
     * @param parametersInfo
     * @param locale
     * @param contentPath
     * @param prefix
     * @param containerItemNode
     * @param containerItemHelper
     * @param componentParameters
     * @param properties the already existing {@link ContainerItemComponentPropertyRepresentation}s
     * @return a <code>ContainerItemComponentPropertyRepresentation</code> instance or <code>null</code> if no
     * {@link ContainerItemComponentPropertyRepresentation} needs to be added (because for example already
     * representation for parameter exists)
     * @throws {@link java.lang.RuntimeException} in case some unexpected exception occurs
     */
    ContainerItemComponentPropertyRepresentation createProperty(
            final ParametersInfo parametersInfo,
            final Locale locale,
            final String contentPath,
            final String prefix,
            final Node containerItemNode,
            final ContainerItemHelper containerItemHelper,
            final HstComponentParameters componentParameters,
            final List<ContainerItemComponentPropertyRepresentation> properties);
}
