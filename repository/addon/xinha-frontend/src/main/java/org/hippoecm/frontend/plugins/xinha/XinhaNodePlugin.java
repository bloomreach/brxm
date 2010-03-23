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
package org.hippoecm.frontend.plugins.xinha;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.plugins.richtext.IImageDecorator;
import org.hippoecm.frontend.plugins.richtext.ILinkDecorator;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.PrefixingImageDecorator;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.RichTextUtil;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.xinha.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.links.InternalLinkBehavior;
import org.hippoecm.frontend.plugins.xinha.dragdrop.XinhaDropBehavior;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImageService;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLinkService;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaNodePlugin extends AbstractXinhaPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(XinhaNodePlugin.class);

    private PreviewLinksBehavior previewLinksBehavior;
    private InternalLinkBehavior linkPickerBehavior;
    private ImagePickerBehavior imagePickerBehavior;

    private XinhaImageService imageService;
    private XinhaLinkService linkService;

    public XinhaNodePlugin(IPluginContext context, final IPluginConfig config) {
        super(context, config);
    }

    private String getPrefix(JcrPropertyValueModel valueModel) {
        String binariesPath = BINARIES_PREFIX
                + valueModel.getJcrPropertymodel().getItemModel().getParentModel().getPath();
        return RichTextUtil.encodeResourceURL(RichTextUtil.encode(binariesPath) + "/");
    }

    @Override
    protected JcrPropertyValueModel getValueModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        return getContentModel(nodeModel);
    }

    @Override
    protected JcrPropertyValueModel getBaseModel() {
        IPluginConfig config = getPluginConfig();
        if (!config.containsKey("model.compareTo")) {
            return null;
        }
        IModelReference modelRef = getPluginContext().getService(config.getString("model.compareTo"),
                IModelReference.class);
        if (modelRef == null || modelRef.getModel() == null) {
            return null;
        }
        return getContentModel((JcrNodeModel) modelRef.getModel());
    }

    @Override
    protected IModel<String> newCompareModel() {
        JcrPropertyValueModel<String> baseModel = getBaseModel();
        JcrNodeModel baseNodeModel = new JcrNodeModel(baseModel.getJcrPropertymodel().getItemModel().getParentModel());
        final IRichTextLinkFactory baseLinkFactory = new JcrRichTextLinkFactory(baseNodeModel);

        JcrPropertyValueModel<String> currentModel = getValueModel();
        JcrNodeModel currentNodeModel = new JcrNodeModel(currentModel.getJcrPropertymodel().getItemModel()
                .getParentModel());
        final IRichTextLinkFactory currentLinkFactory = new JcrRichTextLinkFactory(currentNodeModel);

        // links that are in both: set to current
        // otherwise: set to respective prefix

        String basePrefix = getPrefix(baseModel);
        final IImageDecorator baseDecorator = new PrefixingImageDecorator(basePrefix);
        String currentPrefix = getPrefix(currentModel);
        final IImageDecorator currentDecorator = new PrefixingImageDecorator(currentPrefix);
        IModel<String> decoratedBase = new PrefixingModel(baseModel, new IImageDecorator() {
            private static final long serialVersionUID = 1L;

            public String srcFromSrc(String link) {
                if (baseLinkFactory.getLinks().contains(link) && currentLinkFactory.getLinks().contains(link)) {
                    RichTextLink baseRtl = baseLinkFactory.loadLink(link);
                    RichTextLink currentRtl = currentLinkFactory.loadLink(link);
                    if (baseRtl != null && currentRtl != null && currentRtl.getTargetId().equals(baseRtl.getTargetId())) {
                        return currentDecorator.srcFromSrc(link);
                    }
                }
                return baseDecorator.srcFromSrc(link);
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void detach() {
                baseLinkFactory.detach();
                currentLinkFactory.detach();
                super.detach();
            }
        };
        IModel<String> decoratedCurrent = new PrefixingModel(currentModel, currentDecorator);

        return new BrowsableModel(new DiffModel(decoratedBase, decoratedCurrent), previewLinksBehavior);
    }

    @Override
    protected IModel<String> newViewModel() {
        return new BrowsableModel(new PrefixingModel(getValueModel(), getPrefix(getValueModel())), previewLinksBehavior);
    }

    @Override
    protected IModel<String> newEditModel() {
        RichTextModel model = (RichTextModel) super.newEditModel();
        model.setCleaner(getPluginContext().getService(IHtmlCleanerService.class.getName(), IHtmlCleanerService.class));
        JcrNodeModel nodeModel = new JcrNodeModel(getValueModel().getJcrPropertymodel().getItemModel().getParentModel());
        model.setLinkFactory(new JcrRichTextLinkFactory(nodeModel));
        return new PrefixingModel(model, getPrefix(getValueModel()));
    }

    @Override
    protected Fragment createPreview(String fragmentId) {
        if (previewLinksBehavior == null) {
            add(previewLinksBehavior = new PreviewLinksBehavior());
        }

        return super.createPreview(fragmentId);
    }

    @Override
    protected Fragment createEditor(String fragmentid) {
        Fragment fragment = super.createEditor(fragmentid);

        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        imageService = new XinhaImageService(new JcrRichTextImageFactory(nodeModel), configuration.getName());
        linkService = new XinhaLinkService(new JcrRichTextLinkFactory(nodeModel), configuration.getName());

        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();
        editor.add(imagePickerBehavior = new ImagePickerBehavior(context, config
                .getPluginConfig("Xinha.plugins.InsertImage"), imageService));
        editor.add(linkPickerBehavior = new InternalLinkBehavior(context, config
                .getPluginConfig("Xinha.plugins.CreateLink"), linkService));

        if (previewLinksBehavior != null) {
            remove(previewLinksBehavior);
            previewLinksBehavior = null;
        }

        add(new XinhaDropBehavior(context, config) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void insertImage(JcrNodeModel model, AjaxRequestTarget target) {
                String returnScript = imageService.attach(model);
                if (returnScript != null) {
                    target.getHeaderResponse().renderOnDomReadyJavascript(returnScript);
                }
            }

            @Override
            protected void updateImage(JcrNodeModel model, AjaxRequestTarget target) {
                //TODO: check if old image facet select should be deleted
                insertImage(model, target);
            }

            @Override
            protected void insertLink(JcrNodeModel model, AjaxRequestTarget target) {
                String returnScript = linkService.attach(model);
                if (returnScript != null) {
                    target.getHeaderResponse().renderOnDomReadyJavascript(returnScript);
                }
            }

            @Override
            protected void updateLink(JcrNodeModel model, AjaxRequestTarget target) {
                //TODO: check if old link facet select should be deleted
                insertLink(model, target);
            }
        });

        return fragment;
    }

    private JcrPropertyValueModel getContentModel(JcrNodeModel nodeModel) {
        try {
            Node node = nodeModel.getNode();
            if (node == null) {
                return null;
            }
            Property prop = node.getProperty("hippostd:content");
            return new JcrPropertyValueModel(new JcrPropertyModel(prop));
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public void onBeforeRender() {
        if (configuration != null && configuration.getEditorStarted()) {
            //TODO: add enum to distinguish sorts of drops available
            if (configuration.getPluginConfiguration("InsertImage") != null) {
                configuration.getPluginConfiguration("InsertImage").addProperty("callbackUrl",
                        imagePickerBehavior.getCallbackUrl().toString());
            }

            if (configuration.getPluginConfiguration("CreateLink") != null) {
                configuration.getPluginConfiguration("CreateLink").addProperty("callbackUrl",
                        linkPickerBehavior.getCallbackUrl().toString());
            }
        }
        // TODO Auto-generated method stub
        super.onBeforeRender();
    }

    @Override
    protected void onDetach() {
        if (imageService != null) {
            imageService.detach();
        }
        if (linkService != null) {
            linkService.detach();
        }
        super.onDetach();
    }

    class PreviewLinksBehavior extends AbstractDefaultAjaxBehavior implements ILinkDecorator {
        private static final long serialVersionUID = 1L;

        private static final String JS_STOP_EVENT = "Wicket.stopEvent(event);";

        @Override
        protected void respond(AjaxRequestTarget target) {
            Request request = RequestCycle.get().getRequest();
            String link = request.getParameter("link");
            if (link != null) {
                IBrowseService browser = getPluginContext().getService(
                        getPluginConfig().getString(IBrowseService.BROWSER_ID), IBrowseService.class);
                if (browser != null) {
                    JcrNodeModel model = (JcrNodeModel) getModel();
                    Node node = model.getNode();
                    try {
                        if (node.hasNode(link)) {
                            node = node.getNode(link);
                            if (node.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                                String uuid = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                                javax.jcr.Session s = ((UserSession) getSession()).getJcrSession();
                                node = s.getNodeByUUID(uuid);
                                browser.browse(new JcrNodeModel(node));
                            }
                        }
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public String internalLink(String link) {
            String url = getCallbackUrl(false) + "&link=" + WicketURLEncoder.QUERY_INSTANCE.encode(link);
            return "href=\"#\" onclick=\"" + JS_STOP_EVENT + generateCallbackScript("wicketAjaxGet('" + url + "'")
                    + "\"";
        }

        public String externalLink(String link) {
            return "href=\"" + link + "\" onclick=\"" + JS_STOP_EVENT + "\"";
        }
    }

}
