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
package org.hippoecm.frontend.translation.components.document;

import java.util.List;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.IModel;
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

        add(TranslationResources.getTranslationsHeaderContributor());
        add(JavascriptPackageResource.getHeaderContribution(DocumentTranslationView.class, "translate-document.js"));
        codecBehavior = new NodeNameCodecBehavior(codec);
        add(codecBehavior);
        add(new ExtHippoThemeBehavior());
        add(CSSPackageResource.getHeaderContribution(getClass(), "style.css"));
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
        properties.put("imageService", imageService.getCallbackUrl(false));

        properties.put("emptyImg", rc.urlFor(new ResourceReference(getClass(), "empty.png")));
        properties.put("folderImg", rc.urlFor(new ResourceReference(getClass(), "folder.png")));
        properties.put("documentImg", rc.urlFor(new ResourceReference(getClass(), "doc.png")));
    }

}
