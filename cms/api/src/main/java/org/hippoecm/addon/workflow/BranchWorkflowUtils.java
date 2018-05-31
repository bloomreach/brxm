/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;

public class BranchWorkflowUtils {

    public static Map<String, String> getBranchInfo(final Workflow workflow) throws RepositoryException, RemoteException, WorkflowException {
        return getBranchInfo(workflow.hints());
    }

    public static Map<String, String> getBranchInfo(final Map<String, Serializable> hints) {
        return (Map<String, String>) hints.get("branchVariantsInfo");
    }

    /**
     * @return the branchId of {@code variant} and null if the {@code variant} does not exist
     */
    public static String getBranchId(final Workflow workflow, WorkflowUtils.Variant variant) throws RepositoryException, RemoteException, WorkflowException {
        return getBranchId(workflow.hints(), variant);
    }


    /**
     * @return the branchId for the first existing {@code variants} and null if none of the {@code variants} exist
     */
    public static String getBranchId(final Workflow workflow, WorkflowUtils.Variant... variants) throws RepositoryException, RemoteException, WorkflowException {
        return getBranchId(workflow.hints(), variants);
    }

    /**
     * @return the branchId of the hints for {@code variant} and null if the {@code variant} does not exist
     */
    public static String getBranchId(final Map<String, Serializable> hints, WorkflowUtils.Variant variant) {
        return getBranchId(hints, new WorkflowUtils.Variant[]{variant});
    }

    /**
     * @return the branchId for the first existing {@code variant} and null if none of the {@code variants} exist
     */
    public static String getBranchId(final Map<String, Serializable> hints, WorkflowUtils.Variant... variants) {
        final Map<String, String> branchInfo = getBranchInfo(hints);
        if (branchInfo == null) {
            return null;
        }
        for (WorkflowUtils.Variant variant : variants) {
            final String id = branchInfo.get(variant.getState());
            if (id != null) {
                return id;
            }
        }
        return null;
    }
}
