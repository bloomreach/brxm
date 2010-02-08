/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class PropertiesPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public String publicationDate = null;
    public String lastModificationDate = null;
    public String lastModifiedBy = null;
    public String creationDate = null;
    public String createdBy = null;

    public PropertiesPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(new Label("publicationDate", new PropertyModel(this, "publicationDate")));
        add(new Label("lastModificationDate", new PropertyModel(this, "lastModificationDate")));
        add(new Label("lastModifiedBy", new PropertyModel(this, "lastModifiedBy")));
        add(new Label("creationDate", new PropertyModel(this, "creationDate")));
        add(new Label("createdBy", new PropertyModel(this, "createdBy")));

        onModelChanged();
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        if (getDefaultModel() instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel)getDefaultModel();
            Node node = nodeModel.getNode();
            try {
                if (node != null) {
                    if (node.isNodeType("hippostdpubwf:document")) {
                        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        if (node.hasProperty("hippostdpubwf:publicationDate")) {
                            publicationDate = formatter.format(node.getProperty("hippostdpubwf:publicationDate").getDate());
                        } else {
                            publicationDate = null;
                        }
                        lastModificationDate = formatter.format(node.getProperty("hippostdpubwf:lastModificationDate").getDate().getTime());
                        lastModifiedBy = node.getProperty("hippostdpubwf:lastModifiedBy").getString();
                        creationDate = formatter.format(node.getProperty("hippostdpubwf:creationDate").getDate().getTime());
                        createdBy = node.getProperty("hippostdpubwf:createdBy").getString();
                    }
                }
            } catch (RepositoryException ex) {
                publicationDate = null;
                lastModificationDate = null;
                lastModifiedBy = null;
                creationDate = null;
                createdBy = null;
            }
        }
    }
}
