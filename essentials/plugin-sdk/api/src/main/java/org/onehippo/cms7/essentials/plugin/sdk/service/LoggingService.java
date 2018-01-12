/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.service;

import java.io.File;

/**
 * LoggingService provides methods to manipulate logging configuration files.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface LoggingService {
    /**
     * Add a logger with the specified name and level to all log4j files found in the project.
     *
     * @param configuration Configuration file to update
     * @param loggerName    unique name for the logger
     * @param level         level for the logger
     * @return              true if the logger exists in the configuration file upon returning, false otherwise
     */
    boolean addLoggerToLog4jConfiguration(File configuration, String loggerName, String level);
}
