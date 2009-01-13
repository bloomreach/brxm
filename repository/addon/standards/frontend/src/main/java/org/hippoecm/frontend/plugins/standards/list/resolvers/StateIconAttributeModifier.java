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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.repository.api.HippoNode;

public class StateIconAttributeModifier extends AbstractNodeAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final String PREFIX = "state-";
    private static final String SUFFIX = "-16";


    @Override
    public AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return new CssClassAppender(new Model("icon-16"));
    }

    @Override
    public AttributeModifier[] getCellAttributeModifiers(HippoNode node) throws RepositoryException {
        AttributeModifier[] attributes = new AttributeModifier[2];
        String cssClass = "";
        String summary = "";
        if (node.hasNode(node.getName())) {
            HippoNode variant = (HippoNode) (node.getNode(node.getName()));
            Node canonicalNode = variant.getCanonicalNode();
            if (canonicalNode.hasProperty("hippostd:stateSummary")) {
                cssClass = PREFIX + canonicalNode.getProperty("hippostd:stateSummary").getString() + SUFFIX;
            }
            summary = canonicalNode.getProperty("hippostd:stateSummary").getString();
        }
        attributes[0] = new CssClassAppender(new Model(cssClass));
        attributes[1] = new AttributeAppender("title", new TypeTranslator(new JcrNodeTypeModel("hippostd:publishableSummary")).getValueName("hippostd:stateSummary", new Model(summary)), " ");
        return attributes;
    }
}
