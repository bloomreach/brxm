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
import java.util.Map;

import org.onehippo.repository.branch.BranchConstants;

/**
 * Provides the default implementation of the interface, which is used if the module configuration property
 * branchSelectionClass is not present.
 */
public class BranchSelectionServiceImpl implements BranchSelectionService {

    @Override
    public String getSelectedBranchId(final Map<String, Serializable> contextPayload) {
        return BranchConstants.MASTER_BRANCH_ID;
    }
}
