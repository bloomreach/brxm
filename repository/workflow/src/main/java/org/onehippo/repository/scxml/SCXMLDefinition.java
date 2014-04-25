/**
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

import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.model.SCXML;

/**
 * SCXMLDefinition wraps a specific SCXML state machine {@link #getSCXML() instance} and some extra metadata as the
 * {@link Evaluator} and the concrete location or {@link #getPath()} of the state machine definition needed for the
 * execution of the state machine.
 */
public interface SCXMLDefinition {

    public String getId();

    public String getPath();

    public SCXML getSCXML();

    public Evaluator getEvaluator();
}
