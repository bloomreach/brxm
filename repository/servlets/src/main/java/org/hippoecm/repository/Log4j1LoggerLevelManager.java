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
import java.util.Enumeration;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

@Deprecated
public class Log4j1LoggerLevelManager implements LoggingServlet.LoggerLevelManager {

    private static final List<String> logLevels = Arrays.asList(
            new String[] { "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL" });

    @Override
    public List<String> getLogLevels() {
        return logLevels;
    }

    @Override
    public SortedMap<String, LoggingServlet.LoggerLevelInfo> getLoggerLevelInfosMap() {
        final SortedMap<String, LoggingServlet.LoggerLevelInfo> loggerLevelInfosMap = new TreeMap<>();
        final Enumeration<Logger> loggers = LogManager.getCurrentLoggers();

        while (loggers.hasMoreElements()) {
            final Logger logger = loggers.nextElement();
            final Level level = logger.getLevel();
            final Level effectiveLevel = logger.getEffectiveLevel();

            if (level != null) {
                loggerLevelInfosMap.put(logger.getName(),
                        new LoggingServlet.LoggerLevelInfo(logger.getName(), level.toString()));
            } else {
                loggerLevelInfosMap.put(logger.getName(),
                        new LoggingServlet.LoggerLevelInfo(logger.getName(), null, effectiveLevel.toString()));
            }
        }
        return loggerLevelInfosMap;
    }

    @Override
    public void setLoggerLevel(final String name, final String levelName) {
        final Logger logger = LogManager.getLogger(name);
        final Level level = Level.toLevel(levelName, null);
        if (level != null) {
            logger.setLevel(level);
        } else {
            LoggingServlet.log.warn("Unable to find Level." + levelName + " , not adjusting logger " + name);
        }
    }
}
