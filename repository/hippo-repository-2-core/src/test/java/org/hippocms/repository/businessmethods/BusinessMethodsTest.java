/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.businessmethods;

import com.atomikos.icatch.config.TSInitInfo;
import com.atomikos.icatch.config.UserTransactionService;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import javax.jcr.RepositoryException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import org.apache.jackrabbit.core.XASession;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.jmock.MockObjectTestCase;

public class BusinessMethodsTest extends MockObjectTestCase {
    private HippoRepository server;
    private UserTransactionService uts;
    private UserTransactionManager utm;

    public BusinessMethodsTest() {
        super();
    }

    public BusinessMethodsTest(String name) {
        super(name);
    }

    public void setUp() throws RepositoryException, SystemException, IOException {
        File repoDir = File.createTempFile("repo", "", new File(System.getProperty("user.dir")));
        repoDir.delete();
        repoDir.mkdirs();
        server = HippoRepositoryFactory.getHippoRepository();

        uts = new UserTransactionServiceImp();
        TSInitInfo initInfo = uts.createTSInitInfo();
        Properties initProperties = new Properties();
        initProperties.setProperty("com.atomikos.icatch.service",
                "com.atomikos.icatch.standalone.UserTransactionServiceFactory");
        initProperties.setProperty("com.atomikos.icatch.console_file_name", "tm.out");
        initProperties.setProperty("com.atomikos.icatch.console_file_limit", "-1");
        initProperties.setProperty("com.atomikos.icatch.console_file_count", "1");
        initProperties.setProperty("com.atomikos.icatch.checkpoint_interval", "500");
        initProperties.setProperty("com.atomikos.icatch.output_dir", repoDir.getPath());
        initProperties.setProperty("com.atomikos.icatch.log_base_dir", repoDir.getPath());
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

        utm = new UserTransactionManager();
        utm.setStartupTransactionService(false);
        utm.init();
    }

    public void tearDown() {
        server.close();

        utm.close();

        uts.shutdownWait();
    }

    public void testSingleMethodSuccess() throws RepositoryException, SystemException, NotSupportedException,
            HeuristicMixedException, HeuristicRollbackException, RollbackException {
        XASession session = openSession();
        try {
            TransactionManager tm = utm;
            tm.begin();
            Transaction tx = tm.getTransaction();
            try {
                XAResource sessionXARes = session.getXAResource();
                tx.enlistResource(sessionXARes);
                try {
                    // TODO: get the node
                    // TODO: get the business methods interface/class of the node
                    // TODO: invoke a business method
                } finally {
                    tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
                }

                tx.commit();
            } finally {
                if (tx.getStatus() == Status.STATUS_ACTIVE) {
                    tx.rollback();
                }
            }

            // TODO: verify results
        } finally {
            session.logout();
        }
    }

    public void testTwoMethodsSuccess() throws RepositoryException, SystemException, NotSupportedException,
            HeuristicMixedException, HeuristicRollbackException, RollbackException {
        XASession session = openSession();
        try {
            TransactionManager tm = utm;
            tm.begin();
            Transaction tx = tm.getTransaction();
            try {
                XAResource sessionXARes = session.getXAResource();
                tx.enlistResource(sessionXARes);
                try {
                    // TODO: get the node
                    // TODO: get the business methods interface/class of the node
                    // TODO: invoke the first business method
                    // TODO: invoke the second business method
                } finally {
                    tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
                }

                tx.commit();
            } finally {
                if (tx.getStatus() == Status.STATUS_ACTIVE) {
                    tx.rollback();
                }
            }

            // TODO: verify results
        } finally {
            session.logout();
        }
    }

    public void testTwoMethodsFailure() throws RepositoryException, SystemException, NotSupportedException,
            HeuristicMixedException, HeuristicRollbackException, RollbackException {
        XASession session = openSession();
        try {
            TransactionManager tm = utm;
            tm.begin();
            Transaction tx = tm.getTransaction();
            try {
                XAResource sessionXARes = session.getXAResource();
                tx.enlistResource(sessionXARes);
                try {
                    // TODO: get the node
                    // TODO: get the business methods interface/class of the node
                    // TODO: invoke the first business method
                    // TODO: invoke the second business method

                    // TODO:  throw IllegalStateException if we get here because the second business method should fail
                } finally {
                    tx.delistResource(sessionXARes, XAResource.TMSUCCESS);
                }
            } finally {
                if (tx.getStatus() == Status.STATUS_ACTIVE) {
                    tx.rollback();
                }
            }

            // TODO: verify results
        } finally {
            session.logout();
        }
    }

    private XASession openSession() throws RepositoryException {
        return (XASession) server.login();
    }
}
