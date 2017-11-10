/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Log4j2Utils is used for manipulating log4j XML files on a file system

 */
public class Log4j2Utils {
    private Log4j2Utils() {
        throw new IllegalStateException("Utility class");
    }
    private static Logger log = LoggerFactory.getLogger(Log4j2Utils.class);

    static boolean hasLogger(Document doc, String logger) {
        return !doc.selectNodes("//Configuration/Loggers/Logger[@name='" + logger + "']").isEmpty();
    }

    static boolean hasLogger(File log4jConfig, String logger) {
        Document doc;
        try {
            doc = new SAXReader().read(log4jConfig);
            return hasLogger(doc, logger);
        } catch (DocumentException e) {
            log.error("Error reading {}", log4jConfig.getAbsolutePath(), e);
        }
        return false;
    }

    static void addLogger(Document doc, String name, String level) {
        Element loggers = (Element) doc.selectSingleNode("//Configuration/Loggers");
        Element entry = DocumentHelper.createElement("Logger").addAttribute("name", name).addAttribute("level", level);
        Element root = (Element) doc.selectSingleNode("//Configuration/Loggers/Root");

        loggers.remove(root);
        loggers.add(entry);
        loggers.addText("\n    ") ;
        loggers.add(root);
    }

    public static void addLogger(File log4jConfig, String name, String level) {
        Document doc;
        try {
            doc = new SAXReader().read(log4jConfig);
            addLogger(doc, name, level);
            writeLog4j2(doc, log4jConfig);
        } catch (DocumentException | IOException e) {
            log.error("Error adding logger to {}", log4jConfig.getAbsolutePath(), e);
        }
    }

    public static void addLoggerToLog4j2Files(String loggerName, String loggerLevel) {
        for(File log4jFile : ProjectUtils.getLog4j2Files()) {
            try {
                Document doc = new SAXReader().read(log4jFile);
                if(!hasLogger(doc, loggerName)) {
                    addLogger(doc, loggerName, loggerLevel);
                }
                writeLog4j2(doc, log4jFile);
            } catch (DocumentException | IOException e) {
                log.error("Error writing logger to log4j configuration file " + log4jFile.getAbsolutePath(), e);
            }
        }
    }

    static void writeLog4j2(Document doc, File target) throws IOException {
        FileWriter writer = new FileWriter(target);
        doc.write(writer);
        writer.close();
    }
}
