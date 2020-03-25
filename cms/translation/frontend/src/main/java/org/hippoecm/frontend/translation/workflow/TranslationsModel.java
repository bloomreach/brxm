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

public class TranslationsModel extends LoadableDetachableModel<Translations> implements IModel<Translations>  {


    public static final String ADD_TRANSLATION = "addTranslation";
    private Set<WorkflowDescriptorModel> workflowDescriptorModels;

    /**
     * Default constructor, constructs the model in detached state with no data associated with the
     * model.
     */
    public TranslationsModel() {
        workflowDescriptorModels = new HashSet<>();
    }
    
    public void addWorkflowDescriptorModel(WorkflowDescriptorModel wf) {
        workflowDescriptorModels.add(wf);
    }

    /**
     * Loads and returns the (temporary) model object.
     *
     * @return the (temporary) model object
     */
    @Override
    protected Translations load() {
        Set<String> availableLocales = new TreeSet<>();
        Boolean canAddTranslation = false;
        for (WorkflowDescriptorModel workflowDescriptorModel : workflowDescriptorModels) {
            final Workflow workflow = workflowDescriptorModel.getWorkflow();
            if (workflow instanceof TranslationWorkflow){
                TranslationWorkflow translationWorkflow = (TranslationWorkflow) workflow;
                final Map<String, Serializable> hints;
                try {
                    hints = translationWorkflow.hints();
                    availableLocales.addAll((Collection<? extends String>) hints.get("available"));
                    canAddTranslation = canAddTranslation || canAddTranslation(hints);
                } catch (WorkflowException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }

            }
            else{
                // log scary message
            }

        }
        final boolean finalCanAddTranslation = canAddTranslation;
        return new Translations() {
            @Override
            public Set<String> getAvailableTranslations() {
                return availableLocales;
            }

            @Override
            public Boolean canAddTranslation() {
                return finalCanAddTranslation;
            }
        };
    }

    private boolean canAddTranslation(final Map<String, Serializable> hints) {
        return !hints.containsKey(ADD_TRANSLATION) || hints.get(ADD_TRANSLATION).equals(Boolean.TRUE);
    }
}

