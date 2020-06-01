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

import org.apache.commons.scxml2.SCXMLExecutor;
import org.onehippo.cms7.services.SingletonService;


/**
 * SCXMLExecutorFactory is a {@link SingletonService} responsible for creating Apache Commons {@link SCXMLExecutor}
 * instances based on a provided {@link SCXMLDefinition}.
 */
@SingletonService
public interface SCXMLExecutorFactory {

    public SCXMLExecutor createSCXMLExecutor(SCXMLDefinition scxmlDef) throws SCXMLException;
}
