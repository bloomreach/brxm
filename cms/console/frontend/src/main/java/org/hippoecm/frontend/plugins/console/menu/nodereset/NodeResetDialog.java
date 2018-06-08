/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.nodereset;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;

public class NodeResetDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private String path;

    public NodeResetDialog(IModel<Node> model) {
        super(model);

        add(new Label("message", "Resetting a node means that you undo all changes that were made to it since the system was bootstrapped." +
                " Changes will not be automatically saved so you can inspect the result of resetting first."));

        try {
            final Node node = getModelObject();
            path = node.getPath();
            error("This functionality is not available");
            setOkEnabled(false);
            return;
        } catch (RepositoryException e) {
            error("An unexpected error occurred: " + e.getMessage());
            setOkEnabled(false);
        }
    }

    @Override
    protected void onOk() {
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Reset " + path);
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

}
