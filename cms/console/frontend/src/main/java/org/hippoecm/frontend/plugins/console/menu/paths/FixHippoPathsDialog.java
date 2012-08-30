/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.plugins.console.menu.paths;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixHippoPathsDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(FixHippoPathsDialog.class);

    private boolean automaticSave = true;

    public FixHippoPathsDialog(IModel<Node> model) {
        super(model);
        String path = null;
        try {
            path = getModelObject().getPath();
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
        add(new CheckBox("automatic-save", new PropertyModel<Boolean>(this, "automaticSave")));
        final String message = "The hippo:paths derived property of node " + path
                + " and its subnodes will be recalculated. This might be needed after moving a folder or a document." +
                " Do you want to continue?";
        add(new Label("message", new Model<String>(message)));
        setFocusOnOk();
    }

    @Override
    public void onOk() {
        try {
            getModelObject().accept(new FixHippoPathsVisitor(automaticSave));
            if (automaticSave) {
                UserSession.get().getJcrSession().save();
            }
        } catch (RepositoryException e) {
            log.error("Error during fixing hippo:paths properties", e);
        }
    }
    
    @Override
    public IModel<String> getTitle() {
        return new Model<String>("Fix hippo:paths");
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

}
