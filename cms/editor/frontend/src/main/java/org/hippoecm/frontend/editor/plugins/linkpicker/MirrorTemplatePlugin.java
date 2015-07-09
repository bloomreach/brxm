/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MirrorTemplatePlugin extends RenderPlugin<Node> {
    private static final Logger log = LoggerFactory.getLogger(MirrorTemplatePlugin.class);

    private static final CssResourceReference MIRROR_TEMPLATE_PLUGIN =
            new CssResourceReference(MirrorTemplatePlugin.class, MirrorTemplatePlugin.class.getSimpleName()+".css");
    public static final String ROOT_NODE_ID = "cafebabe-cafe-babe-cafe-babecafebabe";

    private Fragment fragment;

    public MirrorTemplatePlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);
        init(config);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(MIRROR_TEMPLATE_PLUGIN));
    }

    private void init(final IPluginConfig config) {
        final Mode mode = Mode.fromString(config.getString(ITemplateEngine.MODE), Mode.VIEW);
        switch (mode) {
            case EDIT:
                fragment = new Fragment("fragment", "edit", this);
                addOpenLinkPickerLink();
                addButtons();
                break;
            default:
                fragment = new Fragment("fragment", "viewCompare", this);
                addOpenLink();
        }
        add(fragment);
    }

    private void addOpenLinkPickerLink() {
        final IModel<String> displayModel = getLocalizedNameModel();
        final IPluginContext context = getPluginContext();
        DialogLink openPickerLink = new DialogLink("openLinkPickerLink", displayModel, getDialogFactory(context), getDialogService()) {

            @Override
            public boolean isVisible() {
                return hasFilledDocbase();
            }
        };
        openPickerLink.add(TitleAttribute.set(getPathModel()));
        fragment.add(openPickerLink);
    }

    private void addButtons() {
        addSelectButton();
        addOpenButton();
        addClearButton();
    }

    private void addOpenLink() {
        AjaxLink openLink = new AjaxLink("openLink") {

            @Override
            public boolean isVisible() {
                return hasFilledDocbase();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                open();
            }
        };
        openLink.add(new Label("value",getLocalizedNameModel()));
        openLink.add(TitleAttribute.set(getPathModel()));
        openLink.setOutputMarkupId(true);
        fragment.add(openLink);
    }

    private boolean hasFilledDocbase() {
        return StringUtils.isNotEmpty(getPathModel().getObject());
    }

    private void addOpenButton() {
        final AjaxLink openButton = new AjaxLink("open") {
            @Override
            public boolean isVisible() {
                return hasFilledDocbase();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                open();
            }
        };
        openButton.setOutputMarkupId(true);
        fragment.add(openButton);
    }

    private void addSelectButton() {
        final IPluginContext context = getPluginContext();
        fragment.add(new AjaxLink<Void>("select") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                getDialogService().show(createLinkPickerDialog(context));
            }
        });
    }

    private void addClearButton() {
        final AjaxLink clearButton = new AjaxLink<Void>("clear") {
            @Override
            public boolean isVisible() {
                return hasFilledDocbase();
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearModel();
            }
        };
        fragment.add(clearButton);
    }

    private void clearModel() {
        Node node = this.getModelObject();
        try {
            getDocBaseModel().setObject(node.getSession().getRootNode().getIdentifier());
        } catch (RepositoryException e) {
            log.error("Unable to reset docbase to rootnode uuid", e);
        }
        redraw();
    }

    private IDialogFactory getDialogFactory(final IPluginContext context) {
        return () -> createLinkPickerDialog(context);
    }

    /**
     * Create a link picker dialog
     */
    private AbstractDialog<String> createLinkPickerDialog(final IPluginContext context) {
        final JcrPropertyValueModel<String> docbaseModel = getDocBaseModel();
        final IPluginConfig dialogConfig = LinkPickerDialogConfig.fromPluginConfig(getPluginConfig(), docbaseModel);
        final IChainingModel<String> linkPickerModel = new IChainingModel<String>() {
            public String getObject() {
                return docbaseModel.getObject();
            }

            public void setObject(String uuid) {
                getDocBaseModel().setObject(uuid);
                redraw();
            }

            public IModel<String> getChainedModel() {
                return docbaseModel;
            }

            public void setChainedModel(IModel<?> model) {
                throw new UnsupportedOperationException("Value model cannot be changed");
            }

            public void detach() {
                docbaseModel.detach();
            }
        };
        return new LinkPickerDialog(context, dialogConfig, linkPickerModel);
    }

    private String getMirrorPath() {
        Node node = MirrorTemplatePlugin.this.getModelObject();
        try {
            if (node != null && node.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                return getPath(node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
            }
        } catch (ValueFormatException e) {
            log.warn("Invalid value format for docbase " + e.getMessage());
            log.debug("Invalid value format for docbase ", e);
        } catch (PathNotFoundException e) {
            log.warn("Docbase not found " + e.getMessage());
            log.debug("Docbase not found ", e);
        } catch (ItemNotFoundException e) {
            log.info("Docbase " + e.getMessage() + " could not be dereferenced");
        } catch (RepositoryException e) {
            log.error("Invalid docbase " + e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private String getPath(final String docbaseUUID) {
        String path = StringUtils.EMPTY;
        try {
            if (!(docbaseUUID == null || docbaseUUID.equals("") || docbaseUUID.equals(ROOT_NODE_ID))) {
                path = getJCRSession().getNodeByIdentifier(docbaseUUID).getPath();
            }
        } catch (RepositoryException e) {
            log.error("Invalid docbase " + e.getMessage(), e);
        }
        return path;
    }

    private Session getJCRSession() {
        Session session = null;
        Node node = this.getModelObject();
        try {
            session = node.getSession();
        } catch (RepositoryException e) {
            log.error("Invalid docbase " + e.getMessage(), e);
        }
        return session;
    }

    private String getLocalizedName(final String identifier) {
        try {
            Node nodeByIdentifier = getJCRSession().getNodeByIdentifier(identifier);
            HippoNode nodeByIdentifier1 = (HippoNode) nodeByIdentifier;
            return nodeByIdentifier1.getLocalizedName();
        } catch (RepositoryException e) {
            log.error("Cannot get localized name " + e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    private void open() {
        final IPluginConfig config = getPluginConfig();
        final IPluginContext context = getPluginContext();
        final IModel<String> displayModel = getPathModel();
        final String browserId = config.getString("browser.id", "service.browse");
        final IBrowseService browseService = context.getService(browserId, IBrowseService.class);
        final String location = config.getString("option.location", displayModel.getObject());
        if (browseService != null) {
            //noinspection unchecked
            browseService.browse(new JcrNodeModel(location));
        } else {
            log.warn("no browse service found with id '{}', cannot browse to '{}'", browserId, location);
        }
    }

    JcrPropertyValueModel<String> getDocBaseModel() {
        return new JcrPropertyValueModel<>(new JcrPropertyModel<String>(((JcrNodeModel) getModel())
                .getItemModel().getPath()
                + "/hippo:docbase"));
    }

    IModel<String> getPathModel() {
        return new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return getMirrorPath();
            }
        };
    }

    IModel<String> getLocalizedNameModel() {
        return new LoadableDetachableModel<String>() {

            @Override
            protected String load() {
                return getLocalizedName(getDocBaseModel().getObject());
            }

        };
    }

}
