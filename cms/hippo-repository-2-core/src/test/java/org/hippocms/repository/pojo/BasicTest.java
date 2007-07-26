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
package org.hippocms.repository.pojo;

import java.io.InputStream;
import java.util.Properties;

import javax.jcr.Session;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import junit.framework.TestCase;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.embedded.Utilities;
import org.hippocms.repository.workflow.TestServiceImpl;

public class BasicTest extends TestCase {
    public void testPersistence() throws Exception {
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository(System.getProperty("user.dir"));
        HippoRepositoryFactory.setDefaultRepository(repository);
        Session session = repository.login();
        session.getRootNode().addNode("files");
        session.save();
        try {
            Properties properties = new Properties();
            InputStream istream = getClass().getClassLoader().getResourceAsStream("jdo.properties");
            properties.load(istream);
            properties.setProperty("javax.jdo.option.ConnectionURL", "jcr:file:" + System.getProperty("user.dir"));

            PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
            PersistenceManager pm = pmf.getPersistenceManager();
            Transaction tx = pm.currentTransaction();
            boolean transactional = true;

            try {
                if (!transactional) {
                    tx.setNontransactionalRead(true);
                    tx.setNontransactionalWrite(true);
                }

                if (transactional)
                    tx.begin();
                TestServiceImpl obj = new TestServiceImpl();
                obj.setMyContent("bla");
                obj.hasAction1 = true;
                pm.makePersistent(obj);

                if (transactional)
                    tx.commit();
            } finally {
                if (transactional && tx.isActive()) {
                    tx.rollback();
                }
            }

            System.err.println("----------");
            session.refresh(false);
            Utilities.dump(System.err, session.getRootNode().getNode("files"));
            System.err.println("----------");

            String uuid = session.getRootNode().getNode("files").getNodes().nextNode().getUUID();
            try {
                if (transactional)
                    tx.begin();
                TestServiceImpl obj = (TestServiceImpl) pm.getObjectById(new JCROID(uuid));
                assertTrue(obj.hasAction(1));
                assertFalse(obj.hasAction(2));
                assertTrue("bla".equals(obj.getMyContent()));
                if (transactional)
                    tx.commit();
            } finally {
                if (transactional && tx.isActive()) {
                    tx.rollback();
                }
            }

            pm.close();
        } catch (Exception ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            throw ex;
        }
        session.getRootNode().getNode("files").remove();
        session.save();
        session.logout();
    }

}
