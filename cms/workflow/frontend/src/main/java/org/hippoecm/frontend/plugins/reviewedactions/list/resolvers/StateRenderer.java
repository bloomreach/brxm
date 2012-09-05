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
package org.hippoecm.frontend.plugins.reviewedactions.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

public class StateRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        //State property is only available on variants
        if (node.hasNode(node.getName())) {
            Node variant = (node.getNode(node.getName()));
            String state = "unknown";
            if (variant.hasProperty("hippostd:stateSummary")) {
                state = variant.getProperty("hippostd:stateSummary").getString();
            }
            return new Label(id, state);
        }
        return new Label(id);
    }

}
