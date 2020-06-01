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
package org.hippoecm.repository.standardworkflow;

import org.hippoecm.repository.api.Workflow;

/**
 * This work-flow interface can be used to indicate that there are never any active work-flow steps that can be
 * performed on a document.  This is typically used when there is a work-flow based on a super type of a document
 * type, but on a derived sub-type this work-flow should not be available.  In such a case the configuration can
 * indicate to have a more specific work-flow to be applicable to the document type, but that it implements this
 * interface only.
 */
public interface NullWorkflow extends Workflow {

}
