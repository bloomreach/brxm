/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.function.UnaryOperator;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

public class BranchInfoWorkflowPlugin extends AbstractDocumentWorkflowPlugin {
    /**
     * Id and name of the document Info.
     * The id should start with "info", see MenuHierarchy.
     */
    private static final String INFO_DOCUMENT_INFO = "infoDocumentInfo";

    public BranchInfoWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        add(new StdWorkflow<DocumentWorkflow>(INFO_DOCUMENT_INFO, INFO_DOCUMENT_INFO) {

            @Override
            public String getSubMenu() {
                return "info";
            }

            @Override
            protected IModel<String> getTitle() {
                final UnaryOperator<String> resolver = key ->
                        new StringResourceModel(key, BranchInfoWorkflowPlugin.this).getString();
                final Node node = getWorkflow().getNode();
                final RetainableStateSummary retainableStateSummary = new RetainableStateSummary(node, getBranchId());
                final String info = new BranchInfoBuilder(resolver, getBranchInfo(resolver))
                        .draftChanges(retainableStateSummary.hasDraftChanges())
                        .unpublishedChanges(retainableStateSummary.hasUnpublishedChanges())
                        .live(retainableStateSummary.isLive())
                        .build();
                return Model.of(info);
            }

            @Override
            protected void invoke() {
                // do not invoke workflow
            }
        });
    }

    /**
     * @return the branchInfo for the current branch.
     */
    protected String getBranchInfo(final UnaryOperator<String> resolver) {
        return resolver.apply(BranchInfoBuilder.CORE_DOCUMENT_KEY);
    }

}
