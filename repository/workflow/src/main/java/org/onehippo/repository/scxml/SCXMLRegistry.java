/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.services.SingletonService;


/**
 * SCXMLRegistry is a {@link SingletonService} responsible for loading SCXML state machines definitions.
 */
@SingletonService
public interface SCXMLRegistry {

    /**
     * @param id a unique SCXML state machine id
     * @return a specific SCXML state machine instance and some additional metadata based on the provided unique
     * SCXML id.
     */
    public SCXMLDefinition getSCXMLDefinition(String id);
}
