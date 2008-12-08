/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.repository.api.HippoNode;

public class GalleryFolderAttributeModifier extends AbstractNodeAttributeModifier implements IListAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    @Override
    protected AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        String cssClass = "";
        if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
            cssClass = "folder-48";
        }
        return new CssClassAppender(new Model(cssClass));
    }

    @Override
    protected AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return new CssClassAppender(new Model("icon-48"));
    }

}
