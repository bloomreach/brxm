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
package org.hippoecm.repository;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import junit.framework.TestCase;

import org.hippoecm.repository.api.HippoQuery;

public class HippoQuerySample extends TestCase {

    private final static String SVN_ID = "$Id$";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        Node root = session.getRootNode();
        session.save();
    }

    public void tearDown() throws Exception {
        session.save();
        session.logout();
        server.close();
    }

    public void plainExample() throws RepositoryException {
        QueryManager qmgr = session.getWorkspace().getQueryManager();
        {
            Query query = qmgr.createQuery("//zoekiets", Query.XPATH);
            Node node = query.storeAsNode("/query");
        }
        {
            Node node = session.getRootNode().getNode("/query");
            Query q = qmgr.getQuery(node);
            QueryResult rs = q.execute();
            for(RowIterator iter = rs.getRows(); iter.hasNext(); ) {
                Value value = iter.nextRow().getValue("propertyName");
            }
        }
    }

    public void example1() throws RepositoryException {
        QueryManager qmgr = session.getWorkspace().getQueryManager();
        {
            Query query = qmgr.createQuery("//zoekiets[@x='?',@y='?']", Query.XPATH);
            Node node = query.storeAsNode("/query");
        }
        {
            Node node = session.getRootNode().getNode("/query");
            Query q = qmgr.getQuery(node);
            HippoQuery query = (HippoQuery) q;
            QueryResult rs = query.execute(new String[] { "hier", "daar" });
        }
    }

    public void example2() throws RepositoryException {
        QueryManager qmgr = session.getWorkspace().getQueryManager();
        {
            Query query = qmgr.createQuery("//zoekiets[@x='$x',@y='$y',@z='$y']", Query.XPATH);
            Node node = query.storeAsNode("/query");
        }
        {
            Node node = session.getRootNode().getNode("/query");
            Query q = qmgr.getQuery(node);
            HippoQuery query = (HippoQuery) q;
            Map<String,String> arguments = new TreeMap<String,String>();
            arguments.put("y","daar");
            arguments.put("x","hier");
            QueryResult rs = query.execute(arguments);
        }
    }
}
