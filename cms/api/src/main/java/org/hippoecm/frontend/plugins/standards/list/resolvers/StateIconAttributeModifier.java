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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.ObservablePropertyModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StateIconAttributeModifier extends AbstractNodeAttributeModifier {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(StateIconAttributeModifier.class);

    static final String PREFIX = "state-";
    static final String SUFFIX = "-16";

    @Override
    public AttributeModifier getColumnAttributeModifier() {
        return new CssClassAppender(new Model("icon-16"));
    }

    @Override
    public AttributeModifier[] getCellAttributeModifiers(Node node) {
        StateIconAttributes attrs = new StateIconAttributes(new JcrNodeModel(node));
        AttributeModifier[] attributes = new AttributeModifier[2];
        attributes[0] = new CssClassAppender(new ObservablePropertyModel(attrs, "cssClass"));
        attributes[1] = new AttributeAppender("title", new ObservablePropertyModel(attrs, "summary"), " ");
        return attributes;
    }
}
