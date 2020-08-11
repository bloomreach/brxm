/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.AbstractHstComponentParameters;

public class XPageComponentParameters extends AbstractHstComponentParameters {

    public XPageComponentParameters(final Node node, final ContainerItemHelper containerItemHelper) throws RepositoryException {
        super(node, containerItemHelper);
    }

    public void setNodeChanges() throws RepositoryException {
        super.setNodeChanges();
    }

}
