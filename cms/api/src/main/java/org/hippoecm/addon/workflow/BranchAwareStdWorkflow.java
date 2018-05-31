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

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;

public abstract class BranchAwareStdWorkflow extends StdWorkflow implements BranchAware {
    public BranchAwareStdWorkflow(final String id, final String name) {
        super(id, name);
    }

    public BranchAwareStdWorkflow(final String id, final IModel name) {
        super(id, name);
    }

    public BranchAwareStdWorkflow(final String id, final IModel name, final WorkflowDescriptorModel model) {
        super(id, name, model);
    }

    public BranchAwareStdWorkflow(final String id, final IModel name, final IPluginContext pluginContext, final WorkflowDescriptorModel model) {
        super(id, name, pluginContext, model);
    }

    public BranchAwareStdWorkflow(final String id, final IModel name, final ResourceReference iconReference, final WorkflowDescriptorModel model) {
        super(id, name, iconReference, model);
    }

    public BranchAwareStdWorkflow(final String id, final IModel name, final ResourceReference iconReference, final IPluginContext pluginContext, final WorkflowDescriptorModel model) {
        super(id, name, iconReference, pluginContext, model);
    }

    public abstract void updateBranch(final String branchId);
}
