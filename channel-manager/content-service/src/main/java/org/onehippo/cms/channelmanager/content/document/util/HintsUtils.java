/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HintsUtils {

    private static final Logger log = LoggerFactory.getLogger(HintsUtils.class);


    private HintsUtils() {
    }

    /**
     * Gets the hints from the workflow, taking into account the branchId present in the context payload.
     * The hints that are returned can then be passed to the static methods of the EditingUtils class.
     * Please note that the hints are valid until a workflow action has been executed. After execution of
     * a workflow action the hints should be recomputed because the state of the involved nodes usually changes.
     *
     * @param editableWorkflow a workflow
     * @param branchId         branch Id for which to get the hints
     * @return hints
     */
    public static Map<String, Serializable> getHints(EditableWorkflow editableWorkflow, String branchId) {
        try {
            return editableWorkflow.hints(branchId);
        } catch (WorkflowException | RepositoryException | RemoteException e) {
            log.warn("Failed reading hints from workflow", e);
        }
        return Collections.emptyMap();
    }

}
