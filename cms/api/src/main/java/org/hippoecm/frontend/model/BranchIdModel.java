/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.model;

import java.util.Objects;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchIdModel implements IModel<IModelReference<String>> {


    private static final String UNDEFINED = "undefined";
    private static final  Logger log = LoggerFactory.getLogger(BranchIdModel.class);
    private IModelReference<String> branchIdModelReference;

    public BranchIdModel(final IPluginContext context, final String handleIdentifier) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(handleIdentifier);
        this.branchIdModelReference = getOrCreateModelReference(context, getReferenceModelIdentifier(handleIdentifier));
    }

    private static String getUserId() {
        return UserSession.get().getJcrSession().getUserID();
    }

    @Override
    public IModelReference<String> getObject() {
        return branchIdModelReference;
    }

    @Override
    public void setObject(final IModelReference<String> object) {
        this.branchIdModelReference = object;
    }

    public String getBranchId() {
        String result = null;
        if (branchIdModelReference != null) {
            final IModel<String> model = this.branchIdModelReference.getModel();
            if (model != null) {
                result = model.getObject();
            }
        }
        return result;
    }

    public void setBranchId(String branchId) {
        log.debug("Setting branch id to :{}", branchId);
        this.branchIdModelReference.setModel(new Model<>(branchId));
    }

    public void setInitialBranchId(String branchId) {
        if (UNDEFINED.equals(getBranchId())) {
            log.debug("Setting initial branch id to :{}", branchId);
            setBranchId(branchId);
        }
    }

    public void destroy() {
        log.debug("Destroy branch id model reference");
        ((ModelReference) branchIdModelReference).destroy();
    }

    /**
     * Detaches model after use. This is generally used to null out transient references that can be
     * re-attached later.
     */
    @Override
    public void detach() {
    }

    @SuppressWarnings("unchecked")
    private IModelReference<String> getOrCreateModelReference(IPluginContext context, String referenceModelIdentifier) {
        return Optional
                .ofNullable(getBranchIdModelReference(context, referenceModelIdentifier))
                .orElseGet(() -> createBranchIdModelReference(context, referenceModelIdentifier));
    }

    private IModelReference<String> getBranchIdModelReference(final IPluginContext context, final String referenceModelIdentifier) {
        log.debug("Getting model reference for a model contain the branchId for identifier:{}", referenceModelIdentifier);
        return (IModelReference<String>) context.getService(referenceModelIdentifier, IModelReference.class);
    }

    private IModelReference<String> createBranchIdModelReference(final IPluginContext context, final String referenceModelIdentifier) {
        log.debug("Creating model reference for a model containing the branchId for identifier:{}", referenceModelIdentifier);
        ModelReference<String> modelReference = new ModelReference<>(referenceModelIdentifier, new Model<>(UNDEFINED));
        modelReference.init(context);
        return modelReference;
    }

    private String getReferenceModelIdentifier(final String identifier) {
        return identifier + "." + getUserId();
    }


}
