/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.console.menu.refs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferencesDialog extends Dialog<Node> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ReferencesDialog.class);

    public ReferencesDialog(final ReferencesPlugin plugin) {
        setSize(DialogConstants.LARGE);
        
        final IModel<Node> nodeModel = (IModel<Node>) plugin.getDefaultModel();
        setModel(nodeModel);

        add(new RefreshingView<Property>("references") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<Property>> getItemModels() {
                final List<IModel<Property>> refModels = new LinkedList<>();
                final Node node = ReferencesDialog.this.getModelObject();
                try {
                    PropertyIterator refs = node.getReferences();
                    while (refs.hasNext()) {
                        refModels.add(new JcrPropertyModel<>(refs.nextProperty()));
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return refModels.iterator();
            }

            @Override
            protected void populateItem(final Item<Property> item) {
                item.add(new Label("property", new PropertyModel(item.getModel(), "path")));
                AjaxLink link = new AjaxLink("reference") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Property prop = item.getModelObject();
                        if (prop != null) {
                            try {
                                plugin.setModel(new JcrNodeModel(prop.getParent()));
                                closeDialog();
                            } catch (RepositoryException e) {
                                error(e.getMessage());
                                log.error(e.getMessage());
                            }
                        }
                    }

                };
                link.add(new Label("name", new PropertyModel(item.getModel(), "parent.name")));
                item.add(link);
            }

        });
    }

    public IModel<String> getTitle() {
        final IModel<Node> nodeModel = getModel();
        String path;
        try {
            path = nodeModel.getObject().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
            log.warn("Unable to get path for : " + nodeModel);
        }
        return new Model("References for " + path);
    }
}
