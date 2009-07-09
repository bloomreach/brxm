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

package org.hippoecm.hst.plugins.frontend.editor.dao;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.Description;
import org.hippoecm.hst.plugins.frontend.editor.domain.Descriptive;
import org.hippoecm.hst.plugins.frontend.util.IOUtil;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;

public class DescriptionDAO extends EditorDAO<Descriptive> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public static final String HST_DESCRIPTION = "hst:description";
    public static final String HST_ICON = "hst:icon";

    public DescriptionDAO(IPluginContext context, String namespace) {
        super(context, namespace);
    }

    @Override
    public Description load(JcrNodeModel model) {
        Description desc = new Description(model);
        if (JcrUtilities.hasProperty(model, HST_DESCRIPTION)) {
            desc.setDescription(JcrUtilities.getProperty(model, HST_DESCRIPTION));
        }

        if (JcrUtilities.hasProperty(model, HST_ICON)) {
            InputStream is;
            try {
                is = model.getNode().getProperty(HST_ICON).getStream();
                desc.setIconResource(IOUtil.obtainResource(is));
            } catch (ValueFormatException e) {
                log.error("Error loading icon resource into description bean", e);
            } catch (PathNotFoundException e) {
                log.error("Error loading icon resource into description bean", e);
            } catch (RepositoryException e) {
                log.error("Error loading icon resource into description bean", e);
            } catch (IOException e) {
                log.error("Error loading icon resource into description bean", e);
            }
        }
        return desc;
    }

    @Override
    protected void persist(Descriptive k, JcrNodeModel model) {
        JcrUtilities.updateProperty(model, HST_DESCRIPTION, k.getDescription());
        if (k.getIconResource() != null) {
            Node node = model.getNode();
            try {
                if (!node.hasNode(HST_ICON)) {
                    node.addNode(HST_ICON, "hippo:resource");
                }
                JcrNodeModel resourceModel = new JcrNodeModel(node.getNode(HST_ICON));
                IResourceStream r = k.getIconResource().getResourceStream();

                JcrUtilities.updateProperty(resourceModel, "jcr:mimeType", r.getContentType());
                JcrUtilities.updateProperty(resourceModel, "jcr:data", r.getInputStream());
                JcrUtilities.updateProperty(resourceModel, "jcr:lastModified", r.lastModifiedTime().getMilliseconds());
            } catch (RepositoryException e) {
                log.error("Repository error while persisting description data on node "
                        + model.getItemModel().getPath(), e);
            } catch (ResourceStreamNotFoundException e) {
                log.error("Error while retrieving InputStream for property jcr:data on node "
                        + model.getItemModel().getPath(), e);
            }
        }

    }
}
