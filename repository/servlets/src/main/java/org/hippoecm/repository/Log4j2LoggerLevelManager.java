/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class Log4j2LoggerLevelManager implements LoggingServlet.LoggerLevelManager {

    private static final List<String> logLevels = Arrays.stream(Level.values()).map(Level::name).collect(Collectors.toList());

    @Override
    public List<String> getLogLevels() {
        return logLevels;
    }

    @Override
    public SortedMap<String, LoggingServlet.LoggerLevelInfo> getLoggerLevelInfosMap() {
        this.getClass().getClassLoader().getSystemResource("");
        final SortedMap<String, LoggingServlet.LoggerLevelInfo> loggerLevelInfosMap = new TreeMap<>();
        // The following assumes System property -DLog4jContextSelector=org.apache.logging.log4j.core.selector.BasicContextSelector
        // to work propery. See: https://logging.apache.org/log4j/2.0/manual/logsep.html
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        for (Logger logger : context.getLoggers()) {
            final LoggerConfig loggerConfig = config.getLoggerConfig(logger.getName());
            if (loggerConfig.getName().equals(logger.getName())) {
                loggerLevelInfosMap.put(logger.getName(),
                        new LoggingServlet.LoggerLevelInfo(logger.getName(), logger.getLevel().name()));
            } else {
                loggerLevelInfosMap.put(logger.getName(),
                        new LoggingServlet.LoggerLevelInfo(logger.getName(), null, logger.getLevel().name()));
            }
        }
        return loggerLevelInfosMap;
    }

    @Override
    public void setLoggerLevel(final String name, final String level) {
        Configurator.setLevel(name, Level.getLevel(level));
    }
}
