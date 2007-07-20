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
package org.hippocms.repository.jr.embedded;

import java.io.File;
import java.util.Properties;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.icatch.config.TSInitInfo;
import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;

public abstract class HippoRepository {
    private static UserTransactionService uts = null;

    protected Repository repository;
    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    private String systemUsername = "username";
    private String systemPassword = "password";

    private void initialize() {
	// HREPTWO-40: disable because of problems reinitializing.
        // FIXME: bring these properties into resource
        if (uts == null && true==false) { // FIXME not thread safe
            uts = new UserTransactionServiceImp();
            TSInitInfo initInfo = uts.createTSInitInfo();
            Properties initProperties = new Properties();
            initProperties.setProperty("com.atomikos.icatch.service",
                    "com.atomikos.icatch.standalone.UserTransactionServiceFactory");
            initProperties.setProperty("com.atomikos.icatch.console_file_name", "tm.out");
            initProperties.setProperty("com.atomikos.icatch.console_file_limit", "-1");
            initProperties.setProperty("com.atomikos.icatch.console_file_count", "1");
            initProperties.setProperty("com.atomikos.icatch.checkpoint_interval", "500");
            initProperties.setProperty("com.atomikos.icatch.output_dir", getWorkingDirectory());
            initProperties.setProperty("com.atomikos.icatch.log_base_dir", getWorkingDirectory());
            initProperties.setProperty("com.atomikos.icatch.log_base_name", "tmlog");
            initProperties.setProperty("com.atomikos.icatch.max_actives", "50");
            initProperties.setProperty("com.atomikos.icatch.max_timeout", "60000");
            initProperties.setProperty("com.atomikos.icatch.tm_unique_name", "tm");
            initProperties.setProperty("com.atomikos.icatch.serial_jta_transactions", "true");
            initProperties.setProperty("com.atomikos.icatch.automatic_resource_registration", "true");
            initProperties.setProperty("com.atomikos.icatch.console_log_level", "WARN");
            initProperties.setProperty("com.atomikos.icatch.enable_logging", "true");
            initInfo.setProperties(initProperties);
            uts.init(initInfo);
        }
    }

    private String workingDirectory;

    protected HippoRepository() {
        workingDirectory = new File(System.getProperty("user.dir")).getAbsolutePath();
        initialize();
    }

    protected HippoRepository(String workingDirectory) {
        if (workingDirectory == null || workingDirectory.equals(""))
            throw new NullPointerException();
        this.workingDirectory = new File(workingDirectory).getAbsolutePath();
        initialize();
    }

    protected String getWorkingDirectory() {
        return workingDirectory;
    }

    protected String getLocation() {
        return workingDirectory;
    }

    public Session login() throws LoginException, RepositoryException {
        if (systemUsername != null)
            return login(systemUsername, systemPassword);
        else
            return login(null);
    }

    public Session login(String username, String password) throws LoginException, RepositoryException {
        if (username != null && !username.equals(""))
            return login(new SimpleCredentials(systemUsername, systemPassword.toCharArray()));
        else
            return login(systemUsername, systemPassword);
    }

    public Session login(SimpleCredentials credentials) throws LoginException, RepositoryException {
        Session session = null;
        if (credentials == null)
            session = repository.login();
        else
            session = repository.login(credentials);
        if (session != null)
            log.info("Logged in as " + session.getUserID() + " to a "
                    + repository.getDescriptor(Repository.REP_NAME_DESC) + " repository.");
        else if (credentials == null)
            log.error("Failed to login to repository with no credentials");
        else
            log.error("Failed to login to repository with credentials " + credentials.toString());
        return session;
    }

    public void close() {
        if (uts != null) {
            uts.shutdownWait();
            uts = null;
        }
    }
}
