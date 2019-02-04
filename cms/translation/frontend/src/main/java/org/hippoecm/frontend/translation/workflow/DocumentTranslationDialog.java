/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.AccessDeniedException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.addon.workflow.IWorkflowInvoker;
import org.hippoecm.addon.workflow.WorkflowDialog;
import org.hippoecm.addon.workflow.WorkflowSNSException;
import org.hippoecm.frontend.service.ISettingsService;
import org.hippoecm.frontend.translation.ILocaleProvider;
import org.hippoecm.frontend.translation.components.document.DocumentTranslationView;
import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.frontend.util.CodecUtils;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTranslationDialog extends WorkflowDialog<Void> {

    private static final Logger log = LoggerFactory.getLogger(DocumentTranslationDialog.class);

    private static final IValueMap DIALOG_SIZE = new ValueMap("width=675,height=405").makeImmutable();

    private final ISettingsService settingsService;

    public DocumentTranslationDialog(final ISettingsService settings,
                                     final IWorkflowInvoker action,
                                     final IModel<String> title,
                                     final List<FolderTranslation> folders,
                                     final IModel<Boolean> autoTranslateContent,
                                     final String sourceLanguage,
                                     final String targetLanguage,
                                     final ILocaleProvider provider) {
        super(action);
        this.settingsService = settings;

        setTitle(title);
        setSize(DIALOG_SIZE);

        final DocumentTranslationView documentTranslationView = new DocumentTranslationView("grid", folders,
                sourceLanguage, targetLanguage,
                new LoadableDetachableModel<StringCodec>() {
                    @Override
                    protected StringCodec load() {
                        final StringCodecFactory stringCodecFactory = settingsService.getStringCodecFactory();
                        return stringCodecFactory.getStringCodec(CodecUtils.ENCODING_NODE, targetLanguage);
                    }
                }, provider);
        documentTranslationView.setFrame(false);
        add(documentTranslationView);

        if (autoTranslateContent != null) {
            add(new BooleanFieldWidget("translate", autoTranslateContent));
        } else {
            add(new Label("translate").setVisible(false));
        }
    }

    @Override
    protected void onOk() {
        try {
            getInvoker().invokeWorkflow();
        } catch (final WorkflowSNSException e) {
            log.warn("Could not execute workflow due to same-name-sibling issue: " + e.getMessage());
            handleExceptionTranslation(e, e.getConflictingName());
        } catch (final WorkflowException e) {
            log.warn("Could not execute workflow: " + e.getMessage());
            handleExceptionTranslation(e);
        } catch (final AccessDeniedException e) {
            log.warn("Access denied: " + e.getMessage());
            handleExceptionTranslation(e);
        } catch (final Exception e) {
            log.error("Could not execute workflow.", e);
            error(e);
        }
    }

    private void handleExceptionTranslation(final Throwable e, final Object... parameters) {
        final List<String> errors = new ArrayList<>();
        Throwable t = e;
        while(t != null) {
            final String translatedMessage = getExceptionTranslation(t, parameters).getObject();
            if (translatedMessage != null && !errors.contains(translatedMessage)) {
                errors.add(translatedMessage);
            }
            t = t.getCause();
        }
        if (log.isDebugEnabled()) {
            log.debug("Exception caught: {}", StringUtils.join(errors.toArray(), ";"), e);
        }

        errors.stream().forEach(this::error);
    }
}
