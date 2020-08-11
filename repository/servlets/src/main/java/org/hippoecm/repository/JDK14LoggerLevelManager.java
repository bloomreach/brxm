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
import java.util.logging.Level;

public class JDK14LoggerLevelManager implements LoggingServlet.LoggerLevelManager {

    private static final List<String> logLevels = Arrays.asList(
            new String[] { "OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE","FINER", "FINEST", "ALL" });

    @Override
    public List<String> getLogLevels() {
        return logLevels;
    }

    @Override
    public SortedMap<String, LoggingServlet.LoggerLevelInfo> getLoggerLevelInfosMap() {
        SortedMap<String, LoggingServlet.LoggerLevelInfo> loggerLevelInfosMap = new TreeMap<>();
        java.util.logging.LogManager manager = java.util.logging.LogManager.getLogManager();

        for (Enumeration<String> namesIter = manager.getLoggerNames(); namesIter.hasMoreElements();) {
            String loggerName = namesIter.nextElement();
            java.util.logging.Logger logger = manager.getLogger(loggerName);
            java.util.logging.Level level = logger.getLevel();
            java.util.logging.Level effectiveLevel = level;

            // try to find effective level
            if (level == null) {
                for (java.util.logging.Logger l = logger; l != null; l = l.getParent()) {
                    if (l.getLevel() != null) {
                        effectiveLevel = l.getLevel();
                        break;
                    }
                }
            }

            if (level != null) {
                loggerLevelInfosMap.put(loggerName, new LoggingServlet.LoggerLevelInfo(loggerName, level.toString()));
            } else {
                loggerLevelInfosMap.put(loggerName, new LoggingServlet.LoggerLevelInfo(loggerName, null, effectiveLevel.toString()));
            }
        }
        return loggerLevelInfosMap;
    }

    @Override
    public void setLoggerLevel(final String name, final String level) {
        java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
        java.util.logging.Logger logger = logManager.getLogger(name);

        if (logger != null) {
            logger.setLevel(Level.parse(level));
        } else {
            LoggingServlet.log.warn("Logger not found : " + name);
        }
    }
}
