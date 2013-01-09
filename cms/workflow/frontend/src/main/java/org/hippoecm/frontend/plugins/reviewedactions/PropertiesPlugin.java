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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class PropertiesPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    private String publicationDate = null;
    private String lastModificationDate = null;
    private String lastModifiedBy = null;
    private String creationDate = null;
    private String createdBy = null;

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
                        Calendar calendar;
                        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        publicationDate = null;
                        if (node.hasProperty("hippostdpubwf:publicationDate")) {
                            calendar = node.getProperty("hippostdpubwf:publicationDate").getDate();
                            if (calendar.getTimeInMillis() != 0) {
                                publicationDate = formatter.format(calendar.getTime());
                            }
                        }
                        lastModificationDate = null;
                        calendar = node.getProperty("hippostdpubwf:lastModificationDate").getDate();
                        if (calendar.getTimeInMillis() != 0) {
                            lastModificationDate = formatter.format(calendar.getTime());
                        }
                        lastModifiedBy = node.getProperty("hippostdpubwf:lastModifiedBy").getString();
                        creationDate = null;
                        calendar = node.getProperty("hippostdpubwf:creationDate").getDate();
                        if (calendar.getTimeInMillis() != 0) {
                            creationDate = formatter.format(calendar.getTime());
                        }
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
