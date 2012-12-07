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
package org.hippoecm.frontend.translation.workflow;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.AbstractWorkflowDialog;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.components.document.DocumentTranslationView;
import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;

public class DocumentTranslationDialog extends AbstractWorkflowDialog<Void> {

    private static final long serialVersionUID = 1L;

    private IModel<String> title;
    private ISettingsService settingsService;

    public DocumentTranslationDialog(ISettingsService settings,
            IWorkflowInvoker action, IModel<String> title, List<FolderTranslation> folders, IModel<Boolean> autoTranslateContent,
            String sourceLanguage, String targetLanguage,
            ILocaleProvider provider) {
        super(null, action);
        this.settingsService = settings;
        this.title = title;

        DocumentTranslationView dtv = new DocumentTranslationView("grid", folders,
                sourceLanguage, targetLanguage,
                new LoadableDetachableModel<StringCodec>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected StringCodec load() {
                        StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
                        return stringCodecFactory.getStringCodec("encoding.node");
                    }
                }, provider);
        dtv.setFrame(false);
        add(dtv);
        if(autoTranslateContent != null) {
            add(new BooleanFieldWidget("translate", autoTranslateContent));
        } else {
            add(new Label("translate").setVisible(false));
        }
    }

    public IModel getTitle() {
        return title;
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=675,height=405").makeImmutable();
    }
}
