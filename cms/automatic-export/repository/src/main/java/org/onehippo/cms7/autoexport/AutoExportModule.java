/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import java.io.File;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.autoexport.Constants.PROJECT_BASEDIR_PROPERTY;
import static org.onehippo.cms7.autoexport.Constants.OLD_EXPORT_DIR_PROPERTY;
import static org.onehippo.cms7.autoexport.Constants.LOGGER_NAME;

/**
 * This module implements automatic export of repository content.
 * In order to use this functionality you need to set the system property
 * {@code project.basedir} to point to your project's base directory.
 * <p>
 */
public final class AutoExportModule implements DaemonModule {

    static final Logger log = LoggerFactory.getLogger(LOGGER_NAME);
    
    private EventProcessor processor;

    public AutoExportModule() {}

    @Override
    public void initialize(Session session) throws RepositoryException {

        if (System.getProperty(OLD_EXPORT_DIR_PROPERTY) != null && !System.getProperty(OLD_EXPORT_DIR_PROPERTY).isEmpty()) {
            log.warn("Auto export doesn't recognize system property {} anymore. " +
                    "Use {} instead to point to your project base directory.", new Object[] { OLD_EXPORT_DIR_PROPERTY, PROJECT_BASEDIR_PROPERTY});
        }

        File baseDir;
        if (System.getProperty(PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            baseDir = new File(System.getProperty(PROJECT_BASEDIR_PROPERTY));
        } else {
            log.info("System property {} not set. Automatic export will not be available.", PROJECT_BASEDIR_PROPERTY);
            return;
        }
        
        try {
            processor = new EventProcessor(baseDir, session);
        } catch (Exception e) {
            log.error("Failed to initialize auto export. Auto export will not be available", e);
        }

    }

    @Override
    public void shutdown() {
        if (processor != null) {
            processor.shutdown();
        }
    }

}
