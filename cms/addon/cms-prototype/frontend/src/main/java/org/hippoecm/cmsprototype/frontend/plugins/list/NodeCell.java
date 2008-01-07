/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.list;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeCell extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(AbstractListingPlugin.class);


    public NodeCell(String id, NodeModelWrapper model, final Channel channel, String nodePropertyName) {
        super(id, model);
        AjaxLink link = new AjaxLink("link", model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // create a "select" request with the node path as a parameter
                JcrNodeModel nodeModel = ((NodeModelWrapper) this.getModel()).getNodeModel();
                Request request = channel.createRequest("select", nodeModel.getMapRepresentation());
                channel.send(request);
                request.getContext().apply(target);
            }

        };
        addLabel(model, nodePropertyName, link);
        add(link);
    }

    /**
     * Override this getLabel for classes needing parts which cannot be represented in a 
     * jcrNodeModel (like rep:excerpt(.) for a search node cell)
     * @param model
     */
    protected void addLabel(NodeModelWrapper model, String nodePropertyName, AjaxLink link) {
        if(hasDefaultCustomizedLabels(nodePropertyName)) {
            addDefaultCustomizedLabel(model, nodePropertyName, link);
        }
        else if (model.getObject() instanceof HippoNode) {
            try {
                HippoNode n = (HippoNode) model.getObject();

                // hardcoded non-jcrnode properties
                if (nodePropertyName.equals("name")) {
                    addLabel(link, n.getName());
                } else if (nodePropertyName.equals("displayname")) {
                    addLabel(link, n.getDisplayName());
                } else if (nodePropertyName.equals("islocked")) {
                    addLabel(link, String.valueOf(n.isLocked()));
                } else if (nodePropertyName.equals("path")) {
                    addLabel(link, n.getPath());
                }
                else {
                    getLabel4Property(n.getProperty(nodePropertyName), link);
                }
            } catch (ValueFormatException e) {
                log.debug("Unable to find property for culumn " + nodePropertyName + ". Creating empty label. Reason : " + e.getMessage());
                addEmptyLabel(link);
            } catch (PathNotFoundException e) {
                log.debug("Unable to find property for culumn " + nodePropertyName + ". Creating empty label. Reason : " + e.getMessage());
                addEmptyLabel(link);
            } catch (RepositoryException e) {
                log.debug("Unable to find property for culumn " + nodePropertyName + ". Creating empty label. Reason : " + e.getMessage());
                addEmptyLabel(link);
            }
        } else {
            addEmptyLabel(link);
        }
        
    }

    /**
     * If a subclass needs customized label for some property, it needs to override this method
     * @param model
     * @param nodePropertyName
     * @param link
     */
    protected void addDefaultCustomizedLabel(NodeModelWrapper model, String nodePropertyName, AjaxLink link) {
        addEmptyLabel(link);
    }

    /**
     * default never specific behavior. Used for subclasses to add specific behavior
     * @param nodePropertyName
     * @return is properyname has specific behavior, return true
     */
    protected boolean hasDefaultCustomizedLabels(String nodePropertyName) {
        return false;
    }

    protected void getLabel4Property(Property p, AjaxLink link) throws ValueFormatException, RepositoryException {
        switch (p.getType()) {
        case PropertyType.BINARY:
            // never show binary value
            break;
        case PropertyType.BOOLEAN:
            addLabel(link, String.valueOf(p.getBoolean()));
            break;
        case PropertyType.DATE:
            addLabel(link, String.valueOf(p.getDate()));
            break;
        case PropertyType.DOUBLE:
            addLabel(link, String.valueOf(p.getDouble()));
            break;
        case PropertyType.LONG:
            addLabel(link, String.valueOf(p.getLong()));
            break;
        case PropertyType.REFERENCE:
            // do not show references
            break;
        case PropertyType.PATH:
            addLabel(link, String.valueOf(p.getPath()));
            break;
        case PropertyType.STRING:
            addLabel(link, String.valueOf(p.getString()));
            break;
        case PropertyType.NAME:
            addLabel(link, String.valueOf(p.getName()));
            break;
        default:
            throw new IllegalArgumentException("illegal internal value type");
        }
    }

    protected void addLabel(AjaxLink link, String value) {
        link.add(new Label("label", value));
    }

    protected void addEmptyLabel(AjaxLink link) {
        link.add(new Label("label"));
    }
}
