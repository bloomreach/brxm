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

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.hst.plugins.frontend.editor.domain.Template;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateDAO extends EditorDAO<Template> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(TemplateDAO.class);

    public static final String HST_CONTAINERS = "hst:containers";
    public static final String HST_RENDERPATH = "hst:renderpath";

    public TemplateDAO(IPluginContext context, String namespace) {
        super(context, namespace);
    }

    @Override
    public Template load(JcrNodeModel model) {
        Template template = new Template(model);

        try {
            template.setName(model.getNode().getName());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        if (JcrUtilities.hasProperty(model, HST_RENDERPATH)) {
            template.setRenderPath(JcrUtilities.getProperty(model, HST_RENDERPATH));
        }

        if (JcrUtilities.hasProperty(model, HST_CONTAINERS)) {
            template.setContainers(JcrUtilities.getMultiValueProperty(model, HST_CONTAINERS));
        }

        return template;
    }

    @Override
    protected void persist(Template k, JcrNodeModel model) {

        k.setModel(JcrUtilities.rename(model, k.getName()));
        JcrUtilities.updateProperty(model, HST_RENDERPATH, k.getRenderPath());
        JcrUtilities.updateMultiValueProperty(model, HST_CONTAINERS, k.getContainers());
    }

}
