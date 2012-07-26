/*
 *  Copyright 2010 Hippo.
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
import java.util.UUID;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.extjs.ExtHippoThemeBehavior;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.LocaleImageService;
import org.hippoecm.frontend.translation.PathRenderer;
import org.hippoecm.frontend.translation.TranslationResources;
import org.hippoecm.frontend.translation.ILocaleProvider.HippoLocale;
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
import org.wicketstuff.js.ext.util.ExtPropertyConverter;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.Translation.Folder.Panel")
public final class FolderTranslationView extends ExtPanel {

    private static final long serialVersionUID = 1L;

    private final ILocaleProvider provider;
    private final ExtTreeNode root;
    private final ExtTreeLoader loader;
    private final SiblingLocator locator;
    private final LocaleImageService imageService;
    private final SelectionListener selectionListener;
    private final PathRenderer pathRenderer;

    private final IModel<T9Tree> treeModel;
    private final IModel<T9Node> model;

    public FolderTranslationView(String id, final IModel<T9Tree> baseTreeModel, final IModel<T9Node> t9Model,
            ILocaleProvider provider) {
        super(id);

        this.provider = provider;

        add(TranslationResources.getTranslationsHeaderContributor());
        add(TranslationResources.getCountriesCss());
        add(new ExtHippoThemeBehavior());

        addHeaderContribution("treegrid/TreeGridSorter.js");
        addHeaderContribution("treegrid/TreeGridColumnResizer.js");
        addHeaderContribution("treegrid/TreeGridNodeUI.js");
        addHeaderContribution("treegrid/TreeGridLoader.js");
        addHeaderContribution("treegrid/TreeGridColumns.js");
        addHeaderContribution("treegrid/TreeGrid.js");
        addHeaderContribution("folder-translations.js");

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

        root = new ExtAsyncTreeNode();
        root.setId(rootNode.getId());
        root.setText(rootNode.getName());
        add(root);

        T9Node node = model.getObject();
        add(loader = new FolderTree(treeModel, node.getLang(), provider));
        add(locator = new SiblingLocator(treeModel));
        add(imageService = new LocaleImageService(provider));
        add(pathRenderer = new PathRenderer(provider));

        selectionListener = new SelectionListener() {
            private static final long serialVersionUID = 1L;

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

            @Override
            public void detach(Component component) {
                t9IdModel.detach();
                super.detach(component);
            }

            @Override
            public void onSelect(String t9id) {
                if (t9id == null) {
                    t9IdModel.setObject(unlinkedT9Id);
                } else {
                    t9IdModel.setObject(t9id);
                }
            }
        };
        add(selectionListener);
    }

    private void addHeaderContribution(String js) {
        add(JavascriptPackageResource.getHeaderContribution(FolderTranslationView.class, js));
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
        properties.put("imageService", imageService.getCallbackUrl(false));
        properties.put("pathRenderer", new JSONIdentifier(pathRenderer.getJsObjectId()));

        JSONObject locales = new JSONObject();
        for (HippoLocale locale : provider.getLocales()) {
            JSONObject jsonLocale = new JSONObject();
            jsonLocale.put("country", locale.getLocale().getCountry());
            locales.put(locale.getName(), jsonLocale);
        }
        properties.put("locales", getEscapeModelStrings());

        JSONObject listeners;
        if (properties.has("listeners")) {
            listeners = properties.getJSONObject("listeners");
        } else {
            listeners = new JSONObject();
        }
        listeners.put("select-folder", new JSONIdentifier(selectionListener.getJsListener()));
        properties.put("listeners", listeners);

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

//        properties.put("breakLinkDisabled", rc.urlFor(new ResourceReference(getClass(), "broken-link-disabled.png")));
        properties.put("breakLink", rc.urlFor(new ResourceReference(getClass(), "unlink-translations-16.png")));
    }

    @Override
    protected void onDetach() {
        model.detach();
        treeModel.detach();
        super.onDetach();
    }

    private abstract static class SelectionListener extends AbstractDefaultAjaxBehavior {
        private static final long serialVersionUID = 1L;

        private static final String POST_PARAM_T9ID = "t9id";

        @Override
        protected void respond(AjaxRequestTarget target) {
            RequestCycle rc = RequestCycle.get();
            String t9id = rc.getRequest().getParameter(POST_PARAM_T9ID);
            if (t9id == null || "null".equals(t9id)) {
                onSelect(null);
            } else {
                onSelect(t9id);
            }
        }

        public abstract void onSelect(String t9id);

        @Override
        protected CharSequence getCallbackScript() {
            String postBody = String.format("%s='+t9id+'", POST_PARAM_T9ID);
            return generateCallbackScript("wicketAjaxGet('" + getCallbackUrl(true) + "&" + postBody + "'");
        }

        public CharSequence getJsListener() {
            return "function(t9id) { " + getCallbackScript() + ";}";
        }
    }

}
