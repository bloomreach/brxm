/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.standardworkflow;

/**
 * Workflows that are executed as part of workflow events must implement this interface.
 * This ensures that when a workflow event is fired, the repository knows which interface to
 * call automatically.  Only a single fire method is applicable at a specific workflow event
 * that is triggered.  Which one depends on the actual workflow call (which caused the trigger
 * to fire) and condition that is being met.
 * 
 * This interface is an extension to the now deprecated WorkflowEventWorkflow interface, and 
 * in addition sets the category and method names of the Workflow triggering the event
 * before the fire method is invoked.
 * @deprecated to do workflow post processing, use the event bus mechanism instead
 */
@Deprecated
public interface WorkflowEventsWorkflow extends WorkflowEventWorkflow {

    /**
     * This method is called before a workflow event is fired passing in the concrete category name of the Worfklow
     * triggering this event.
     */
    public void setWorkflowCategory(String category);

    /**
     * This method is called before a workflow event is fired passing in the concrete method name of the Worfklow
     * triggering this event.
     */
    public void setWorkflowMethod(String method);
}
