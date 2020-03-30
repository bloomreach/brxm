/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.translation.workflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.translation.TranslationWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Determines the union of the hints given by the {@link TranslationWorkflow}.
 * </p>
 * <p>The {@link TranslationWorkflow} operates on nodes of type hippotranslation:translated, which
 * are in most cases the variants under a handle. The menu in the editor displays the combined information
 * of all variants under a handle.</p>
 */
final class TranslationsModel extends LoadableDetachableModel<Translations> {

    /** Key of hint whose value is a boolean that indicates if for a document variant adding a translation is allowed. */
    private static final String ADD_TRANSLATION = "addTranslation";
    /** Key of hint whose value is list of Strings of available locales. */
    private static final String AVAILABLE = "available";
    private static final Logger log = LoggerFactory.getLogger(TranslationsModel.class);
    /** set of workflow descriptor models that are united by this class. */
    private Set<WorkflowDescriptorModel> workflowDescriptorModels = new HashSet<>();

    public void addWorkflowDescriptorModel(final WorkflowDescriptorModel wf) {
        workflowDescriptorModels.add(wf);
    }

    @Override
    protected Translations load() {
        Set<String> availableLocales = new TreeSet<>();
        boolean canAddTranslation = false;
        for (WorkflowDescriptorModel workflowDescriptorModel : workflowDescriptorModels) {
            final Workflow workflow = workflowDescriptorModel.getWorkflow();
            if (workflow instanceof TranslationWorkflow) {
                TranslationWorkflow translationWorkflow = (TranslationWorkflow) workflow;
                try {
                    final Map<String, Serializable> hints = translationWorkflow.hints();
                    availableLocales.addAll((Collection<? extends String>) hints.get(AVAILABLE));
                    canAddTranslation = canAddTranslation || canAddTranslation(hints);
                } catch (WorkflowException | RemoteException | RepositoryException e) {
                    log.warn("Could not get hints for workflow");
                }
            }
        }
        return Translations.of(availableLocales, canAddTranslation);
    }

    private boolean canAddTranslation(final Map<String, Serializable> hints) {
        return !hints.containsKey(ADD_TRANSLATION) || hints.get(ADD_TRANSLATION).equals(Boolean.TRUE);
    }
}

