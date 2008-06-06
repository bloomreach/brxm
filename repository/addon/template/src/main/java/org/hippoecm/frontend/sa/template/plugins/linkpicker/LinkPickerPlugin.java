/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.template.plugins.linkpicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.DialogLink;
import org.hippoecm.frontend.sa.dialog.IDialogFactory;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME: porting this plugin hasn't been completed yet
public class LinkPickerPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private JcrPropertyValueModel valueModel;

    private List<String> nodetypes = new ArrayList<String>();
    private DialogLink linkPicker;
    //private Model linkText;

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    public LinkPickerPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        IDialogService dialogService = getDialogService();
        valueModel = (JcrPropertyValueModel) getModel();
        
        if (config.getString("nodetypes") != null) {
            String[] nodeTypes = config.getStringArray("nodetypes"); 
            nodetypes.addAll(Arrays.asList(nodeTypes));
        }
        if (nodetypes.size() == 0) {
            log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
        }
        
        
        //linkText.setObject("");

        IDialogFactory dialogFactory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService service) {
                return new LinkPickerDialog(LinkPickerPlugin.this, context, service, valueModel, nodetypes);
            }
        };
        add(new DialogLink("value", new Model("linktext"), dialogFactory, dialogService));
        
        setOutputMarkupId(true);
    }

    @Override
    public void onModelChanged() {
        JcrNodeModel newModel = (JcrNodeModel)getModel();
        Node newNode = newModel.getNode();        
        //linkText.setObject(getValue(newModel.getNode()));
    }

    private String getValue(Node jcrNode) {
        String docbaseUUID = (String) valueModel.getObject();
        if(docbaseUUID == null || docbaseUUID.equals("")) {
            return "[...]";
        }
        try {
            return jcrNode.getSession().getNodeByUUID(docbaseUUID).getPath();
        } catch (ValueFormatException e) {
            log.error("invalid docbase" + e.getMessage());
        } catch (PathNotFoundException e) {
            log.error("invalid docbase" + e.getMessage());
        } catch (RepositoryException e) {
            log.error("invalid docbase" + e.getMessage());
        }
        return "[...]";      
    }
}
