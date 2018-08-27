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

package org.onehippo.repository.documentworkflow;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.scxml.MockAccessManagedSession;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;

public class DocumentVariantBuilder {

    static private final String UNDEFINED = "UNDEFINED";
    final private MockAccessManagedSession session;
    final private MockNode handle;
    private final Map<String, DocumentVariant> documentVariantsMap;
    private String relPath = UNDEFINED;
    private String branchId = UNDEFINED;
    private String permissions = UNDEFINED;
    private String holder = UNDEFINED;
    private String state = UNDEFINED;

    public DocumentVariantBuilder(final MockAccessManagedSession session, final MockNode handle, final Map<String, DocumentVariant> documentVariantMap) {
        this.session = session;
        this.handle = handle;
        this.documentVariantsMap = documentVariantMap;
    }

    public DocumentVariantBuilder relPath(String relPath){
        this.relPath = relPath;
        return this;
    }

    public DocumentVariantBuilder branchId(String branchId){
        this.branchId = branchId;
        return this;
    }

    public DocumentVariantBuilder permissions(String permissions){
        this.permissions = permissions;
        return this;
    }

    public DocumentVariantBuilder holder(String holder){
        this.holder = holder;
        return this;
    }

    public DocumentVariantBuilder state(String state){
        this.state = state;
        return this;
    }


    public DocumentVariant add() throws RepositoryException {
        if (documentVariantsMap == null){
            throw new IllegalStateException("DocumentVariantsMap should not be null");
        }
        if (UNDEFINED.equals(state)){
            throw new IllegalStateException("A documentvariant always needs a state, please supply a valid state('published','unpublished' or 'draft'");
        }
        if (UNDEFINED.equals(relPath)){
            throw new IllegalStateException("A documentvariant always needs a relPath, please supply a valid relPath");
        }
        if (!UNDEFINED.equals(holder) && (!HippoStdNodeType.DRAFT.equals(state))){
            throw new IllegalStateException("Only draft can have a holder, change state to draft or do not set holder");
        }
        final MockNode node = handle.addNode(relPath, "hippo:variant");
        if (!UNDEFINED.equals(branchId)){
            node.addMixin(HIPPO_MIXIN_BRANCH_INFO);
            node.setProperty(HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, branchId);
        }
        if (!UNDEFINED.equals(permissions)){
            session.setPermissions(node.getPath(), permissions, true);
        }
        final DocumentVariant variant = new DocumentVariant(node);
        if (!UNDEFINED.equals(holder)){
            variant.setHolder(holder);
        }

        variant.setState(state);
        documentVariantsMap.put(state,variant);
        return variant;
    }
}
