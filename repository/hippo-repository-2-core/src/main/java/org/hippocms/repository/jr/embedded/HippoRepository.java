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

import java.util.List;
import java.util.Iterator;
import java.util.Properties;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import com.atomikos.icatch.config.TSInitInfo;
import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.NamespaceException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.PathNotFoundException;
import javax.jcr.ItemExistsException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.LoginException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.XASession;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeDefImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.PropDefImpl;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippocms.repository.jr.servicing.Service;
import org.hippocms.repository.jr.servicing.ServicingDecoratorFactory;
import org.hippocms.repository.jr.servicing.client.ClientServicesAdapterFactory;
import org.hippocms.repository.jr.servicing.server.ServerServicingAdapterFactory;

public abstract class HippoRepository {
    private static UserTransactionService uts = null;

    protected Repository repository;
    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    private String systemUsername = "username";
    private String systemPassword = "password";

    private void initialize() {
        // FIXME: bring these properties into resource
        if (uts == null) { // FIXME not thread safe
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
