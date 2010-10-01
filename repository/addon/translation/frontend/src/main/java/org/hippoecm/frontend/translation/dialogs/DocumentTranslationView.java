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
package org.hippoecm.frontend.translation.dialogs;

import java.util.List;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.translation.FolderTranslation;
import org.json.JSONException;
import org.onehippo.wicketstuff.extjs.ExtPanel;
import org.onehippo.wicketstuff.extjs.data.ExtJsonStore;
import org.onehippo.wicketstuff.extjs.util.JSONIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTranslationView extends ExtPanel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentTranslationView.class);

    private ExtJsonStore<FolderTranslation> store;
    private AbstractAjaxBehavior codecBehavior;

    public DocumentTranslationView(String id, final List<FolderTranslation> translations) {
        super(id);

        store = new FolderTranslationStore(translations);
        add(store);

        add(JavascriptPackageResource.getHeaderContribution(DocumentTranslationView.class, "translate-document.js"));
        codecBehavior = new NodeNameCodecBehavior();
        add(codecBehavior);
    }

    @Override
    protected String getExtClass() {
        return "HippoTranslator";
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);

        RequestCycle rc = RequestCycle.get();
        try {
            properties.put("store", new JSONIdentifier("store"));
            properties.put("codecUrl", codecBehavior.getCallbackUrl());
            properties.put("imgLeft", rc.urlFor(new ResourceReference(getClass(), "en.png")));
            properties.put("imgRight", rc.urlFor(new ResourceReference(getClass(), "fr.png")));
            properties.put("folderName", new StringResourceModel("folder-name", this, null).getString());
            properties.put("urlName", new StringResourceModel("url-name", this, null).getString());
            properties.put("addTranslation", new StringResourceModel("add-translation", this, null).getString());
            properties.put("emptyImg", rc.urlFor(new ResourceReference(getClass(), "empty.png")));
            properties.put("folderImg", rc.urlFor(new ResourceReference(getClass(), "folder.png")));
            properties.put("documentImg", rc.urlFor(new ResourceReference(getClass(), "doc.png")));
        } catch (JSONException e) {
            log.error("Could not construct configuration for Ext class HippoTranslator, " + e.getMessage());
        }

        super.preRenderExtHead(js);
    }

}
