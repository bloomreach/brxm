package org.onehippo.cms7.essentials.dashboard.wiki.dao;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;
import org.onehippo.cms7.essentials.dashboard.wiki.model.TestNewsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TestHippoNewsDocumentJcrDao extends AbstractJcrDAO<TestNewsDocument>{

    private static Logger log = LoggerFactory.getLogger(TestHippoNewsDocumentJcrDao.class);


    public TestHippoNewsDocumentJcrDao(final Jcrom jcrom) {
        super(jcrom);
    }

    public TestHippoNewsDocumentJcrDao(final Session session, final Jcrom jcrom) {
        super(session, jcrom);
    }

    public TestHippoNewsDocumentJcrDao(final Class<TestNewsDocument> entityClass, final Jcrom jcrom) {
        super(entityClass, jcrom);
    }

    public TestHippoNewsDocumentJcrDao(final Class<TestNewsDocument> entityClass, final Session session, final Jcrom jcrom) {
        super(entityClass, session, jcrom);
    }

    public TestHippoNewsDocumentJcrDao(final Class<TestNewsDocument> entityClass, final Session session, final Jcrom jcrom, final String[] mixinTypes) {
        super(entityClass, session, jcrom, mixinTypes);
    }
}
