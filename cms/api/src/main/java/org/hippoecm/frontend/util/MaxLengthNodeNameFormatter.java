/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;

public class MaxLengthNodeNameFormatter extends MaxLengthStringFormatter {

    private static final long serialVersionUID = 1L;

    public MaxLengthNodeNameFormatter() {
        super();
    }

    public MaxLengthNodeNameFormatter(int maxLength, String split, int indentLength) {
        super(maxLength, split, indentLength);
    }

    public boolean isTooLong(IModel<Node> nodeModel) {
        return isTooLong(nodeModel, 0);
    }

    public boolean isTooLong(IModel<Node> nodeModel, int indent) {
        return super.isTooLong(getName(nodeModel), indent);
    }

    public String parse(IModel<Node> nodeModel, int indent) {
        return super.parse(getName(nodeModel), indent);
    }

    public String parse(IModel<Node> nodeModel) {
        return parse(nodeModel, 0);
    }

    protected String getName(IModel<Node> nodeModel) {
        return new NodeTranslator(nodeModel).getNodeName().getObject();
    }
}
