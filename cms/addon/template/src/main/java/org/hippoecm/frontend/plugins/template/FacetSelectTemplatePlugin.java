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
package org.hippoecm.frontend.plugins.template;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateDescriptor;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.model.properties.JcrPropertiesProvider;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugins.template.FacetJcrPropertyValueGroupProvider.PropertyValueGroup;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;

public class FacetSelectTemplatePlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final String[] relatedFields = new String[] { HippoNodeType.HIPPO_FACETS, HippoNodeType.HIPPO_VALUES,
            HippoNodeType.HIPPO_MODES };

    //private static final Set propSet = new HashSet<String>(Arrays.asList(properties));

    public FacetSelectTemplatePlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel model = (TemplateModel) getPluginModel();
        //TemplateDescriptor descriptor = model.getTemplateDescriptor();

        TemplateModel tmplModel = (TemplateModel) getPluginModel();
        Node jcrNode = tmplModel.getJcrNodeModel().getNode();

        //JcrPropertiesProvider jcrPropertiesProvider = new JcrPropertiesProvider(tmplModel.getJcrNodeModel());

        IDataProvider jcrPropertiesGroupProvider = new FacetJcrPropertyValueGroupProvider(tmplModel.getJcrNodeModel(),
                relatedFields);
        
        FacetSelectView facetselectproperties = new FacetSelectView("facetselectproperties", jcrPropertiesGroupProvider);
        add(facetselectproperties);

        String docbasePath = getDocBasePath(jcrNode);

        add(new Label("docbase", docbasePath));
        setOutputMarkupId(true);
    }

    @Override
    public Plugin addChild(PluginDescriptor childDescriptor) {
        if (!childDescriptor.getWicketId().equals(HippoNodeType.HIPPO_ITEM)) {
            return super.addChild(childDescriptor);
        }
        return null;
    }

    private String getDocBasePath(Node jcrNode) {
        try {
            String docbaseUUID = jcrNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
            return jcrNode.getSession().getNodeByUUID(docbaseUUID).getPath();
        } catch (ValueFormatException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        return "";
    }

    class FacetSelectView extends DataView {

        private static final long serialVersionUID = 1L;
        
        public FacetSelectView(String id, IDataProvider dataProvider) {
            super(id, dataProvider);
            if(dataProvider.size()==0) {
                // add default 'add group' button
            }
        }

        protected void populateItem(Item item) {

            PropertyValueGroup pvg = (PropertyValueGroup) item.getModel();
            try {
                for (int i = 0; i < pvg.getPropertyGroup().length; i++) {
                    Property property = pvg.getPropertyGroup()[i];
                    JcrPropertyModel jcrPropertyModel = new JcrPropertyModel(property);
                    JcrPropertyValueModel jcrPropertyValueModel;
                    jcrPropertyValueModel = new JcrPropertyValueModel(pvg.getIndex(), property.getValues()[pvg
                            .getIndex()], jcrPropertyModel);
                    item.add(new TextFieldWidget(property.getName().substring("hippo:".length()), jcrPropertyValueModel));
                }
            } catch (ValueFormatException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }

//        private AjaxLink addLink(String id, final JcrPropertyModel model) {
//            return new AjaxLink(id, model) {
//                private static final long serialVersionUID = 1L;
//
//                @Override
//                public void onClick(AjaxRequestTarget target) {
//                    try {
//                        Property prop = model.getProperty();
//                        Value[] oldValues = prop.getValues();
//                        String[] newValues = new String[oldValues.length + 1];
//                        for (int i = 0; i < oldValues.length; i++) {
//                            newValues[i] = oldValues[i].getString();
//                        }
//                        newValues[oldValues.length] = "...";
//                        prop.setValue(newValues);
//                    } catch (RepositoryException e) {
//                        //log.error(e.getMessage());
//                    }
//                    FacetSelectView facetSelectView = (FacetSelectView) findParent(FacetSelectView.class);
//                    target.addComponent(facetSelectView);
//                }
//            };
//        }

    }

}
