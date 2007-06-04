package org.hippocms.repository.pojo;

import java.io.File;
import java.io.IOException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import junit.framework.TestCase;
import java.util.Properties;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.PersistenceManager;
import javax.jdo.JDOHelper;

import javax.jdo.Transaction;
import java.io.InputStream;

import org.jpox.store.OID;

import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.Utilities;
import org.hippocms.repository.workflows.TestService;
import org.hippocms.repository.workflows.TestServiceImpl;

/**
 * @version $Id$
 */
public class BasicTest extends TestCase
{
  public void testPersistence() throws Exception {
    HippoRepository repository = (new HippoRepositoryFactory()).getHippoRepository(System.getProperty("user.dir"));
    HippoRepositoryFactory.setDefaultRepository(repository);
    Session session = repository.login();
    session.getRootNode().addNode("files");
    session.save();
    try {
      Properties properties = new Properties();
      InputStream istream = getClass().getClassLoader().getResourceAsStream("jdo.properties");
      properties.load(istream);
      properties.setProperty("javax.jdo.option.ConnectionURL","jcr:file:"+System.getProperty("user.dir"));

      PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
      PersistenceManager pm = pmf.getPersistenceManager();
      Transaction tx = pm.currentTransaction();
      boolean transactional = true;

      try {
        if(!transactional) {
          tx.setNontransactionalRead(true);
          tx.setNontransactionalWrite(true);
        }

        if(transactional)
          tx.begin();
        TestServiceImpl obj = new TestServiceImpl();
        obj.setMyContent("bla");
        obj.hasAction1 = true;
        pm.makePersistent(obj);

        if(transactional)
          tx.commit();
      } finally {
        if(transactional && tx.isActive()) {
          tx.rollback();
        }
      }

      System.err.println("----------");
      session.refresh(false);
      Utilities.dump(System.err, session.getRootNode().getNode("files"));
      System.err.println("----------");

      String uuid = session.getRootNode().getNode("files").getNodes().nextNode().getUUID();
      try {
        if(transactional)
          tx.begin();
        TestServiceImpl obj = (TestServiceImpl) pm.getObjectById(new JCROID(uuid));
        assertTrue(obj.hasAction(1));
        assertFalse(obj.hasAction(2));
        assertTrue("bla".equals(obj.getMyContent()));
        if(transactional)
          tx.commit();
      } finally {
        if(transactional && tx.isActive()) {
          tx.rollback();
        }
      }

      pm.close();
    } catch(Exception ex) {
      System.err.println(ex.getClass().getName()+": "+ex.getMessage());
      ex.printStackTrace(System.err);
      throw ex;
    }
    session.getRootNode().getNode("files").remove();
    session.save();
    session.logout();
  }

}
