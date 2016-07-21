/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.forge.poll.component;

import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

public interface PollComponentInfo {

    @Parameter(name = "poll-dataPath", displayName = "Path to store poll data")
    String getPollDataPath();

    @Parameter(name = "poll-docsClass", displayName = "Poll documents class")
    String getPollDocsClass();

    @Parameter(name = "poll-pollCompoundName", displayName = "Name of poll compound")
    String getPollCompoundName();

    @Parameter(name = "poll-docsPath", required = true, displayName = "Path to poll documents relative to site content root")
    @JcrPath(pickerSelectableNodeTypes = "hippostd:folder")
    String getPollDocsPath();
}
