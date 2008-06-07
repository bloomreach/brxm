/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.legacy.plugin.channel;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class Request extends Message {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Request.class);

    public Request(String operation, IPluginModel model) {
        super(operation, model);
    }
}
