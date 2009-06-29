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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component;
import org.hippoecm.hst.plugins.frontend.editor.domain.Component.Parameter;
import org.hippoecm.hst.plugins.frontend.util.JcrUtilities;

public class ComponentDAO extends EditorDAO<Component> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String HST_REFERENCECOMPONENT = "hst:referencecomponent";
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_SERVERESOURCEPATH = "hst:serveresourcepath";
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";
    private static final String HST_TEMPLATE = "hst:template";

    public ComponentDAO(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public Component load(JcrNodeModel model) {
        Component component = new Component(model);

        //Load name
        try {
            component.setName(model.getNode().getName());
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        if (JcrUtilities.hasProperty(model, HST_REFERENCECOMPONENT)) {
            String name = JcrUtilities.getProperty(model, HST_REFERENCECOMPONENT);
            if (name != null && !"".equals(name)) {
                component.setReference(true);
                component.setReferenceName(getHstContext().component.decodeReferenceName(name));
            }
        }

        //load template
        if (JcrUtilities.hasProperty(model, HST_TEMPLATE)) {
            component.setTemplate(JcrUtilities.getProperty(model, HST_TEMPLATE));
        }

        //load component class name
        if (JcrUtilities.hasProperty(model, HST_COMPONENTCLASSNAME)) {
            component.setComponentClassName(JcrUtilities.getProperty(model, HST_COMPONENTCLASSNAME));
        }

        //load server resource path
        if (JcrUtilities.hasProperty(model, HST_SERVERESOURCEPATH)) {
            component.setServerResourcePath(JcrUtilities.getProperty(model, HST_SERVERESOURCEPATH));
        }

        List<String> names = JcrUtilities.getMultiValueProperty(model, HST_PARAMETERNAMES);
        if (names != null && names.size() > 0) {
            List<String> values = JcrUtilities.getMultiValueProperty(model, HST_PARAMETERVALUES);
            for (int i = 0; i < names.size(); i++) {
                component.addParameter(names.get(i), values.get(i));
            }
        }

        return component;
    }

    @Override
    protected void persist(Component component, JcrNodeModel model) {

        component.setModel(JcrUtilities.rename(model, component.getName()));

        //save componentClassName
        JcrUtilities.updateProperty(model, HST_COMPONENTCLASSNAME, component.getComponentClassName());

        //save server resource path
        JcrUtilities.updateProperty(model, HST_SERVERESOURCEPATH, component.getServerResourcePath());

        //update parameters
        //- hst:parameternames (string) multiple
        //- hst:parametervalues (string) multiple
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        for (Parameter param : component.getParameters()) {
            names.add(param.getName());
            values.add(param.getValue());
        }
        JcrUtilities.updateMultiValueProperty(model, HST_PARAMETERNAMES, names);
        JcrUtilities.updateMultiValueProperty(model, HST_PARAMETERVALUES, values);

        //Create containers
        updateTemplate(component, model);

        //Save template

    }

    private void updateTemplate(Component component, JcrNodeModel model) {
        String templateName = component.getTemplate();
        JcrUtilities.updateProperty(model, HST_TEMPLATE, templateName);

        JcrNodeModel template = new JcrNodeModel(getHstContext().template.absolutePath(templateName));
        List<String> containers = JcrUtilities.getMultiValueProperty(template, TemplateDAO.HST_CONTAINERS);

        Node node = model.getNode();
        Set<String> nodes = new HashSet<String>();
        try {
            if (node.hasNodes()) {
                NodeIterator it = node.getNodes();
                while (it.hasNext()) {
                    nodes.add(it.nextNode().getName());
                }
            }

            if (containers != null) {
                for (String container : containers) {
                    if (nodes.contains(container)) {
                        nodes.remove(container);
                        continue;
                    }
                    node.addNode(container, "hst:component");
                }
            }
            for (String name : nodes) {
                node.getNode(name).remove();
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

}
