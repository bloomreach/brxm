/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.documentworkflow;

import javax.jcr.Node;

import org.hippoecm.repository.api.WorkflowException;

/**
 * DocumentHandleFactory is an optional factory interface to be used to override the default
 * {@link DocumentHandle} instance creation for the DocumentWorkflowImpl
 *
 * @see DocumentWorkflowImpl#createDocumentHandle(javax.jcr.Node)
 */
public interface DocumentHandleFactory {

    /**
     * Factory method to create a DocumentHandle instance
     * @param node The JCR node representing the document handle
     * @return a DocumentHandle instance
     * @throws WorkflowException
     */
    DocumentHandle createDocumentHandle(Node node) throws WorkflowException;
}
