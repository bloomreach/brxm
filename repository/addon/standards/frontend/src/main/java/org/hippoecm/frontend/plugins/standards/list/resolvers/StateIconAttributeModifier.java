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
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;
import org.hippoecm.repository.api.HippoNode;

public class StateIconAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "state-";
    private static final String SUFFIX = "-16";

    @Override
    public AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        String cssClass = "";
        if (node.hasNode(node.getName())) {
            HippoNode variant = (HippoNode) (node.getNode(node.getName()));
            Node canonicalNode = variant.getCanonicalNode();
            if (canonicalNode.hasProperty("hippostd:stateSummary")) {
                cssClass = PREFIX + canonicalNode.getProperty("hippostd:stateSummary").getString() + SUFFIX;
            }
        }
        return new CssClassAppender(new Model(cssClass));
    }

    @Override
    public AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return new CssClassAppender(new Model("icon-16"));
    }

}
