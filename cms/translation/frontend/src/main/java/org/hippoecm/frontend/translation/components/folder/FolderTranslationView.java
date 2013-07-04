/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.components.folder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.extjs.ExtHippoThemeBehavior;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
import org.hippoecm.frontend.translation.LocaleImageService;
import org.hippoecm.frontend.translation.PathRenderer;
import org.hippoecm.frontend.translation.TranslationResources;
import org.hippoecm.frontend.translation.components.folder.model.EditedT9Tree;
import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.hippoecm.frontend.translation.components.folder.model.T9Tree;
import org.hippoecm.frontend.translation.components.folder.service.FolderTree;
import org.hippoecm.frontend.translation.components.folder.service.SiblingLocator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.tree.ExtAsyncTreeNode;
import org.wicketstuff.js.ext.tree.ExtTreeLoader;
import org.wicketstuff.js.ext.tree.ExtTreeNode;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtEventListener;
import org.wicketstuff.js.ext.util.ExtPropertyConverter;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.Translation.Folder.Panel")
public final class FolderTranslationView extends ExtPanel {

    private static final long serialVersionUID = 1L;

    private static final String POST_PARAM_T9ID = "t9id";

    private final ILocaleProvider provider;
    private final ExtTreeNode root;
    private final ExtTreeLoader loader;
    private final SiblingLocator locator;
    private final LocaleImageService imageService;
    private final PathRenderer pathRenderer;

    private final IModel<T9Tree> treeModel;
    private final IModel<T9Node> model;

    public FolderTranslationView(String id, final IModel<T9Tree> baseTreeModel, final IModel<T9Node> t9Model,
            ILocaleProvider provider) {
        super(id);

        this.provider = provider;

        this.model = t9Model;
        this.treeModel = new LoadableDetachableModel<T9Tree>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected T9Tree load() {
                return new EditedT9Tree(baseTreeModel.getObject(), t9Model.getObject());
            }

            @Override
            protected void onDetach() {
                baseTreeModel.detach();
                t9Model.detach();
                super.onDetach();
            }

        };
        T9Node rootNode = treeModel.getObject().getRoot();

        add(new ExtHippoThemeBehavior());

        root = new ExtAsyncTreeNode();
        root.setId(rootNode.getId());
        root.setText(rootNode.getName());
        add(root);

        T9Node node = model.getObject();
        add(loader = new FolderTree(treeModel, node.getLang(), provider));
        add(locator = new SiblingLocator(treeModel));
        add(imageService = new LocaleImageService(provider));
        add(pathRenderer = new PathRenderer(provider));

        addEventListener("select-folder", new ExtEventListener() {

            private final IModel<String> t9IdModel = new PropertyModel<String>(model, "t9id");
            private final String unlinkedT9Id;

            {
                String t9Id = t9IdModel.getObject();
                List<T9Node> siblings = treeModel.getObject().getSiblings(t9Id);
                if (siblings.size() > 0) {
                    unlinkedT9Id = UUID.randomUUID().toString();
                } else {
                    unlinkedT9Id = t9IdModel.getObject();
                }
            }

            public void onSelect(String t9id) {
                if (t9id == null) {
                    t9IdModel.setObject(unlinkedT9Id);
                } else {
                    t9IdModel.setObject(t9id);
                }
            }

            @Override
            public void onEvent(final AjaxRequestTarget target, Map<String, JSONArray> parameters) {
                if (parameters.containsKey(POST_PARAM_T9ID)) {
                    JSONArray values = parameters.get(POST_PARAM_T9ID);
                    if (values.length() > 0) {
                        try {
                            onSelect(values.getString(0));
                        } catch (JSONException e) {
                            throw new RuntimeException("Could not retrieve t9id from select-folder event", e);
                        }
                    } else {
                        onSelect(null);
                    }
                } else {
                    onSelect(null);
                }
            }
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {

        TranslationResources.getTranslationsHeaderContributor().renderHead(response);
        TranslationResources.getCountriesCss().renderHead(response);

        renderJavaScriptReference(response, "treegrid/TreeGridSorter.js");
        renderJavaScriptReference(response, "treegrid/TreeGridColumnResizer.js");
        renderJavaScriptReference(response, "treegrid/TreeGridNodeUI.js");
        renderJavaScriptReference(response, "treegrid/TreeGridLoader.js");
        renderJavaScriptReference(response, "treegrid/TreeGridColumns.js");
        renderJavaScriptReference(response, "treegrid/TreeGrid.js");
        renderJavaScriptReference(response, "folder-translations.js");

        super.renderHead(response);
    }

    private void renderJavaScriptReference(IHeaderResponse response, String js) {
        response.render(JavaScriptHeaderItem.forReference(new JavaScriptResourceReference(FolderTranslationView.class, js)));
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        root.onRenderExtHead(js);
        locator.onRenderExtHead(js);
        loader.onRenderExtHead(js);
        pathRenderer.onRenderExtHead(js);

        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        RequestCycle rc = RequestCycle.get();
        properties.put("root", new JSONIdentifier(root.getJsObjectId()));
        properties.put("loader", new JSONIdentifier(loader.getJsObjectId()));
        properties.put("locator", new JSONIdentifier(locator.getJsObjectId()));
        properties.put("imageService", imageService.getCallbackUrl());
        properties.put("pathRenderer", new JSONIdentifier(pathRenderer.getJsObjectId()));

        JSONObject locales = new JSONObject();
        for (HippoLocale locale : provider.getLocales()) {
            JSONObject jsonLocale = new JSONObject();
            jsonLocale.put("country", locale.getLocale().getCountry());
            locales.put(locale.getName(), jsonLocale);
        }
        properties.put("locales", getEscapeModelStrings());

        T9Node folderNode = model.getObject();
        JSONObject folder = new JSONObject();
        folder.put("lang", folderNode.getLang());
        JSONObject siblings = locator.getSiblingsAsJSON(folderNode.getT9id());
        if (siblings.length() > 0) {
            folder.put("siblings", siblings);
        }
        List<T9Node> ancestors = treeModel.getObject().getPath(folderNode.getId());
        JSONArray jsonPath = new JSONArray();
        for (T9Node ancestor : ancestors) {
            JSONObject jsonElement = new JSONObject();
            ExtPropertyConverter.addProperties(ancestor, ancestor.getClass(), jsonElement);
            jsonPath.put(jsonElement);
        }
        folder.put("path", jsonPath);
        properties.put("folder", folder);

//        properties.put("breakLinkDisabled", rc.urlFor(new PackageResourceReference(getClass(), "broken-link-disabled.png")));
        properties.put("breakLink", rc.urlFor(new ResourceReferenceRequestHandler(
                new PackageResourceReference(getClass(), "unlink-translations-16.png"))));
    }

    @Override
    protected void onDetach() {
        model.detach();
        treeModel.detach();
        super.onDetach();
    }

}
