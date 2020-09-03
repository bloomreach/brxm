/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.api.Workflow;

/**
 * In case an Experiment Page Document is locked, this contributor can be added by downstream projects to provide a
 * lock reason, for example that the Experience Page is under review
 */
public interface ComponentLockedReasonContributor {

    Optional<String> findReason(final HstRequestContext requestContext,
                                final HstComponentConfiguration compConfig,
                                final Workflow workflow,
                                final Map<String, Serializable> hints,
                                final String branchId);
}
