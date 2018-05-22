/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.scxml;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.model.ModelException;
import org.onehippo.repository.documentworkflow.DocumentVariant;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.onehippo.repository.documentworkflow.DocumentVariant.MASTER_BRANCH_ID;


public class BranchFeedbackAction extends AbstractAction {

    private static final long serialVersionUID = 1L;
    // do not change since used in downstream projects!
    public static final String BRANCH_VARIANTS_INFO = "branchVariantsInfo";

    public String getUnpublished() {
        return getParameter("unpublishedExpr");
    }

    @SuppressWarnings("unused")
    public void setUnpublished(String unpublished) {
        setParameter("unpublishedExpr", unpublished);
    }

    public String getPublished() {
        return getParameter("publishedExpr");
    }

    @SuppressWarnings("unused")
    public void setPublished(String published) {
        setParameter("publishedExpr", published);
    }

    public String getDraft() {
        return getParameter("draftExpr");
    }

    @SuppressWarnings("unused")
    public void setDraft(String draft) {
        setParameter("draftExpr", draft);
    }

    private Map<String, DocumentVariant> getVariants() throws ModelException, SCXMLExpressionException {
        final Map<String, DocumentVariant> variants = new HashMap<>();
        final DocumentVariant unpublished = eval(getUnpublished());
        if (unpublished != null) {
            variants.put(UNPUBLISHED, unpublished);
        }
        final DocumentVariant published = eval(getPublished());
        if (published != null) {
            variants.put(PUBLISHED, published);
        }
        final DocumentVariant draft = eval(getDraft());
        if (draft != null) {
            variants.put(DRAFT, draft);
        }
        return variants;
    }

    @Override
    protected void doExecute(ActionExecutionContext exctx) throws ModelException, SCXMLExpressionException {
        if (getVariants().isEmpty()) {
            return;
        }

        final Session workflowSession = getSCXMLWorkflowContext().getWorkflowContext().getInternalWorkflowSession();

        HashMap<String, String> branchFeedback = new HashMap<>();

        for (Map.Entry<String, DocumentVariant> entry : getVariants().entrySet()) {
            try {
                final Node variantNode = entry.getValue().getNode(workflowSession);
                if (variantNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
                    branchFeedback.put(entry.getKey(), variantNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
                } else {
                    branchFeedback.put(entry.getKey(), MASTER_BRANCH_ID);
                }
            } catch (RepositoryException e) {
                throw new ModelException(e);
            }
        }

        getSCXMLWorkflowContext().getFeedback().put(BRANCH_VARIANTS_INFO, branchFeedback);

    }
}
