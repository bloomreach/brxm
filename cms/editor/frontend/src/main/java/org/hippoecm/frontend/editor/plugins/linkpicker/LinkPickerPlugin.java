/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.dialog.ClearableDialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.util.JcrConstants.ROOT_NODE_ID;

public class LinkPickerPlugin extends RenderPlugin<String> {

    private static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    private static final String EMPTY_LINK_TEXT = "[...]";

    private final IModel<String> valueModel;
    private final List<String> nodetypes = new ArrayList<>();

    public LinkPickerPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        valueModel = getModel();

        final IModel<String> displayModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                String docbaseUUID = valueModel.getObject();
                if (docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.equals(ROOT_NODE_ID)) {
                    return EMPTY_LINK_TEXT;
                }
                try {
                    return UserSession.get().getJcrSession().getNodeByIdentifier(docbaseUUID).getPath();
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase {}", e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found {}", e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (ItemNotFoundException e) {
                    log.info("Docbase not found {}", e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase {}", e.getMessage(), e);
                }
                return EMPTY_LINK_TEXT;
            }
        };

        final IEditor.Mode mode = IEditor.Mode.fromString(config.getString("mode"), IEditor.Mode.VIEW);
        if (mode == IEditor.Mode.EDIT) {
            final IDialogFactory dialogFactory = () -> {
                final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), (JcrPropertyValueModel) valueModel);
                return new LinkPickerDialog(context, dialogConfig, new IModel<>() {

                    @Override
                    public String getObject() {
                        return valueModel.getObject();
                    }

                    @Override
                    public void setObject(String object) {
                        valueModel.setObject(object);
                        redraw();
                    }

                    @Override
                    public void detach() {
                        valueModel.detach();
                    }

                });
            };

            add(new ClearableDialogLink("value", displayModel, dialogFactory, getDialogService()) {

                @Override
                public void onClear() {
                    valueModel.setObject(ROOT_NODE_ID);
                    redraw();
                }

                @Override
                public boolean isClearVisible() {
                    // Checking for string literals ain't pretty. It's probably better to create a better display model.
                    return !EMPTY_LINK_TEXT.equals(displayModel.getObject());
                }
            });

        } else {
            add(new Label("value", displayModel));
        }
        setOutputMarkupId(true);
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

}
