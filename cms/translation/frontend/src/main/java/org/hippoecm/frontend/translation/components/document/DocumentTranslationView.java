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
package org.hippoecm.frontend.translation.components.document;

import java.util.List;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceReferenceRequestHandler;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.extjs.ExtHippoThemeBehavior;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.LocaleImageService;
import org.hippoecm.frontend.translation.TranslationResources;
import org.hippoecm.repository.api.StringCodec;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.Translation.Document")
public class DocumentTranslationView extends ExtPanel {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentTranslationView.class);
    private static final PackageResourceReference EMPTY_PNG = new PackageResourceReference(DocumentTranslationView.class, "empty.png");
    private static final PackageResourceReference FOLDER_PNG = new PackageResourceReference(DocumentTranslationView.class, "folder.png");
    private static final PackageResourceReference DOCUMENT_PNG = new PackageResourceReference(DocumentTranslationView.class, "doc.png");
    private static final JavaScriptResourceReference TRANSLATE_DOCUMENT_JS = new JavaScriptResourceReference(DocumentTranslationView.class, "translate-document.js");
    private static final CssResourceReference TRANSLATE_DOCUMENT_SKIN = new CssResourceReference(DocumentTranslationView.class, "style.css");

    private ExtJsonStore<FolderTranslation> store;
    private AbstractAjaxBehavior codecBehavior;
    private LocaleImageService imageService;

    @SuppressWarnings("unused")
    @ExtProperty
    private final String sourceLanguage;

    @SuppressWarnings("unused")
    @ExtProperty
    private final String targetLanguage;

    public DocumentTranslationView(String id, final List<FolderTranslation> translations, String sourceLanguage,
            String targetLanguage, IModel<StringCodec> codec, ILocaleProvider provider) {
        super(id);
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;

        store = new FolderTranslationStore(translations);
        add(store);

        imageService = new LocaleImageService(provider);
        add(imageService);

        codecBehavior = new NodeNameCodecBehavior(codec);
        add(codecBehavior);

        add(new ExtHippoThemeBehavior());
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        TranslationResources.getTranslationsHeaderContributor().renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(TRANSLATE_DOCUMENT_JS));
        response.render(CssHeaderItem.forReference(TRANSLATE_DOCUMENT_SKIN));
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);

        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(org.json.JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);

        RequestCycle rc = RequestCycle.get();
        properties.put("store", new JSONIdentifier(store.getJsObjectId()));
        properties.put("codecUrl", codecBehavior.getCallbackUrl());
        properties.put("imageService", imageService.getCallbackUrl());

        properties.put("emptyImg", rc.urlFor(new ResourceReferenceRequestHandler(EMPTY_PNG)));
        properties.put("folderImg", rc.urlFor(new ResourceReferenceRequestHandler(FOLDER_PNG)));
        properties.put("documentImg", rc.urlFor(new ResourceReferenceRequestHandler(DOCUMENT_PNG)));
    }

}
