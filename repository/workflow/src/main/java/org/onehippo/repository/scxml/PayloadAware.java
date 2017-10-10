/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.onehippo.repository.scxml;

import java.util.Map;
/**
 * PayloadReceivable defines the availabilty of an initial payload.
 *
 * {@link SCXMLWorkflowExecutor#start(Map)} sets the initial payload
 * if the instance of {@link SCXMLWorkflowData} implements this interface.
 */
public interface PayloadAware {

    Map<String,Object> getInitialPayload();

    void setInitialPayload(Map<String,Object> initialPayload);
}
