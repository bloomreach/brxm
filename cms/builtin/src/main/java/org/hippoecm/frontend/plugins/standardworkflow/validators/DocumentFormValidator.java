/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.standardworkflow.validators;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;

abstract class DocumentFormValidator implements IFormValidator {

    /**
     * Return true if <code>parentNode</code> contains a child having the same localized name with the specified
     * <code>localizedName</code>
     */
    protected boolean existedLocalizedName(final Node parentNode, final String localizedName) throws RepositoryException {
        final NodeIterator children = parentNode.getNodes();
        while(children.hasNext()) {
            Node child = children.nextNode();
            if (child.isNodeType(HippoStdNodeType.NT_FOLDER) || child.isNodeType(HippoNodeType.NT_HANDLE)) {
                NodeTranslator nodeTranslator = new NodeTranslator(new JcrNodeModel(child));
                String localizedChildName = nodeTranslator.getNodeName().getObject();
                if (StringUtils.equals(localizedChildName, localizedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected abstract void showError(final String key, Object... parameters);
}
