/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.api;

/**
 * An WorkflowException is thrown when a workflow implementation disallows the workflow step to be taken for some reason. 
 */
public class WorkflowException extends Exception
{

    /**
     * Constructs a new workflow exception with the specified detail message and without initialized cause
     * @param message  the detail message, for later retrieval though getMessage()
     */
    public WorkflowException(String message) {
        super(message);
    }

    /**
     * Constructs a new workflow exception with the specified detail message and cause
     * @param message the detail message, for later retrieval though getMessage()
     * @param reason the cause (A null value is permitted, and indicates that the cause is nonexistent or unknown)
     */
    public WorkflowException(String message, Exception reason) {
        super(message, reason);
    }
}
