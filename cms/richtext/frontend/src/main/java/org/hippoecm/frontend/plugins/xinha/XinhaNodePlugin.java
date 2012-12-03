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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Request;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WicketURLDecoder;
import org.apache.wicket.protocol.http.WicketURLEncoder;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.richtext.IHtmlCleanerService;
import org.hippoecm.frontend.plugins.richtext.IImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.ILinkDecorator;
import org.hippoecm.frontend.plugins.richtext.IRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.IRichTextLinkFactory;
import org.hippoecm.frontend.plugins.richtext.RichTextException;
import org.hippoecm.frontend.plugins.richtext.RichTextImageURLProvider;
import org.hippoecm.frontend.plugins.richtext.RichTextLink;
import org.hippoecm.frontend.plugins.richtext.RichTextModel;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextImageFactory;
import org.hippoecm.frontend.plugins.richtext.jcr.JcrRichTextLinkFactory;
import org.hippoecm.frontend.plugins.standards.diff.HtmlDiffModel;
import org.hippoecm.frontend.plugins.xinha.dialog.images.ImagePickerBehavior;
import org.hippoecm.frontend.plugins.xinha.dialog.links.InternalLinkBehavior;
import org.hippoecm.frontend.plugins.xinha.dragdrop.XinhaDropBehavior;
import org.hippoecm.frontend.plugins.xinha.services.images.XinhaImageService;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLinkService;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XinhaNodePlugin extends AbstractXinhaPlugin {

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

    @Override
    protected JcrPropertyValueModel getValueModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getDefaultModel();
        return getContentModel(nodeModel);
    }

    @Override
    protected JcrPropertyValueModel getBaseModel() {
        return getContentModel(getBaseNodeModel());
    }

    private JcrNodeModel getBaseNodeModel() {
        IPluginConfig config = getPluginConfig();
        if (!config.containsKey("model.compareTo")) {
            log.warn("No model.compareTo reference configured");
            return null;
        }
        IModelReference modelRef = getPluginContext().getService(config.getString("model.compareTo"),
                IModelReference.class);
        if (modelRef == null || modelRef.getModel() == null) {
            log.warn("The configured model.compareTo service is not available or does provide a valid node model");
            return null;
        }
        return (JcrNodeModel) modelRef.getModel();
    }

    @Override
    protected IModel<String> newCompareModel() {
        JcrNodeModel baseNodeModel = getBaseNodeModel();
        if (baseNodeModel == null) {
            return newViewModel();
        }
        JcrPropertyValueModel<String> baseModel = getContentModel(baseNodeModel);
        final IRichTextLinkFactory baseLinkFactory = new JcrRichTextLinkFactory(baseNodeModel);
        IRichTextImageFactory baseImageFactory = new JcrRichTextImageFactory(baseNodeModel);

        JcrPropertyValueModel<String> currentModel = getValueModel();
        JcrNodeModel currentNodeModel = new JcrNodeModel(currentModel.getJcrPropertymodel().getItemModel()
                .getParentModel());
        final IRichTextLinkFactory currentLinkFactory = new JcrRichTextLinkFactory(currentNodeModel);
        IRichTextImageFactory currentImageFactory = new JcrRichTextImageFactory(currentNodeModel);

        // links that are in both: set to current
        // otherwise: set to respective prefix

        final IImageURLProvider baseDecorator = new RichTextImageURLProvider(baseImageFactory, baseLinkFactory);
        final IImageURLProvider currentDecorator = new RichTextImageURLProvider(currentImageFactory, currentLinkFactory);
        IModel<String> decoratedBase = new PrefixingModel(baseModel, new IImageURLProvider() {
            private static final long serialVersionUID = 1L;

            public String getURL(String link) throws RichTextException {
                String facetName = link;
                if (link.indexOf('/') > 0) {
                    facetName = link.substring(0, link.indexOf('/'));
                }
                if (baseLinkFactory.getLinks().contains(facetName) && currentLinkFactory.getLinks().contains(facetName)) {
                    RichTextLink baseRtl = baseLinkFactory.loadLink(facetName);
                    RichTextLink currentRtl = currentLinkFactory.loadLink(facetName);
                    if (currentRtl.getTargetId().equals(baseRtl.getTargetId())) {
                        return currentDecorator.getURL(link);
                    }
                } else if (baseLinkFactory.getLinks().contains(facetName)) {
                    return baseDecorator.getURL(link);
                } else if (currentLinkFactory.getLinks().contains(facetName)) {
                    return currentDecorator.getURL(link);
                }
                return facetName;
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

        return new BrowsableModel(
                new HtmlDiffModel(new StripScriptModel(decoratedBase), new StripScriptModel(decoratedCurrent)),
                previewLinksBehavior);
    }

    @Override
    protected IModel<String> newViewModel() {
        IRichTextImageFactory imageFactory = new JcrRichTextImageFactory((JcrNodeModel) getModel());
        IRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory((JcrNodeModel) getModel());
        IImageURLProvider urlProvider = new RichTextImageURLProvider(imageFactory, linkFactory);
        return new BrowsableModel(new PrefixingModel(super.newViewModel(), urlProvider), previewLinksBehavior);
    }

    @Override
    protected IModel<String> newEditModel() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();
        IRichTextImageFactory imageFactory = new JcrRichTextImageFactory(nodeModel);
        IRichTextLinkFactory linkFactory = new JcrRichTextLinkFactory(nodeModel);

        RichTextModel model = (RichTextModel) super.newEditModel();
        model.setCleaner(getPluginContext().getService(IHtmlCleanerService.class.getName(), IHtmlCleanerService.class));
        model.setLinkFactory(linkFactory);

        IImageURLProvider urlProvider = new RichTextImageURLProvider(imageFactory, linkFactory);
        return new PrefixingModel(model, urlProvider);
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
                .getPluginConfig("Xinha.plugins.CreateLink"),
                config.getAsBoolean(DISABLE_OPEN_IN_A_NEW_WINDOW_CONFIG, false),
                linkService));

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
            Property prop = node.getProperty(getPluginConfig().getString("content.property.name", "hippostd:content"));
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
            link = WicketURLDecoder.PATH_INSTANCE.decode(link);
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
                                javax.jcr.Session s = getSession().getJcrSession();
                                node = s.getNodeByIdentifier(uuid);
                                browser.browse(new JcrNodeModel(node));
                            }
                        }
                    } catch (ItemNotFoundException ex) {
                        log.info("Could not resolve link", ex);
                    } catch (RepositoryException e) {
                        log.error("Error while browing to link", e);
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
