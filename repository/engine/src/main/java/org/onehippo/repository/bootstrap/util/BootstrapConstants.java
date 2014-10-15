/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.bootstrap.util;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.bootstrap.InitializationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BootstrapConstants {

    private BootstrapConstants() {}

    public static Logger log = LoggerFactory.getLogger(InitializationProcessor.class.getPackage().getName());

    public static final String INIT_FOLDER_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH;

    public static final String ITEM_STATUS_DONE = "done";
    public static final String ITEM_STATUS_FAILED = "failed";

}
