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

import org.onehippo.repository.bootstrap.InitializationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.CONFIGURATION_PATH;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LOCK;
import static org.hippoecm.repository.api.HippoNodeType.INITIALIZE_PATH;
import static org.hippoecm.repository.api.HippoNodeType.TEMPORARY_PATH;

public class BootstrapConstants {

    private BootstrapConstants() {}

    public static Logger log = LoggerFactory.getLogger(InitializationProcessor.class.getPackage().getName());

    public static final String SYSTEM_RELOAD_PROPERTY = "repo.bootstrap.reload-on-startup";

    public static final String INIT_FOLDER_PATH = "/" + CONFIGURATION_PATH + "/" + INITIALIZE_PATH;
    public static final String TEMP_FOLDER_PATH = "/"  + CONFIGURATION_PATH + "/" + TEMPORARY_PATH;
    public static final String INIT_LOCK_PATH = INIT_FOLDER_PATH + "/" + HIPPO_LOCK;
    public static final String ITEM_STATUS_DONE = "done";
    public static final String ITEM_STATUS_MISSING = "missing";
    public static final String ITEM_STATUS_FAILED = "failed";
    public static final String ITEM_STATUS_PENDING = "pending";
    public static final String ITEM_STATUS_RELOAD = "reload";

    public static final String ERROR_MESSAGE_RELOAD_DISABLED = "Reload requested but not enabled";
}
