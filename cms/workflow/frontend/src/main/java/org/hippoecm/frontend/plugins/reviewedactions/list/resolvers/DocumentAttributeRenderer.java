/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

public abstract class DocumentAttributeRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = -2853371369162994945L;

    @Override
    protected Component getViewer(String id, final Node node) throws RepositoryException {
        return new Label(id, new MyModel(node)); 
    }

    protected abstract String getObject(StateIconAttributes atts);

    class MyModel extends AbstractReadOnlyModel<String> {

        StateIconAttributes atts;

        public MyModel(Node node) {
            atts = new StateIconAttributes(new JcrNodeModel(node));
        }

        @Override
        public String getObject() {
            return DocumentAttributeRenderer.this.getObject(atts);
        }

        @Override
        public void detach() {
            atts.detach();
        }
        
    }

}
