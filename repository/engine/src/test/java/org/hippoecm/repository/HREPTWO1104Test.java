/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HREPTWO1104Test extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(HREPTWO1104Test.class);

    final private static int stepsize = 10;
    final private static int numiters = 3000;


    protected static final String SYSTEMUSER_ID = "admin";
    protected static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    @Ignore
    public void testRemote() throws RepositoryException {
        int i;
        Runtime runtime = Runtime.getRuntime();
        long snapshot, after, before = runtime.freeMemory();
        for (i = 0; i < numiters; i++) {
            if (i % stepsize == 0) {
                runtime.gc();
                snapshot = runtime.freeMemory();
                log.info("hippo\t" + (i / stepsize) + "\t" + snapshot);
            }
            HippoRepository repository = HippoRepositoryFactory
                    .getHippoRepository("rmi://localhost:1099/hipporepository");
            Session session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session.logout();
            repository.close();
        }
        runtime.gc();
        after = runtime.freeMemory();
        log.info("remote\t" + (i / stepsize) + "\t" + after);
    }

    @Ignore
    public void testLocal1() throws Exception {
        int i;
        Runtime runtime = Runtime.getRuntime();
        long snapshot, after, before = runtime.freeMemory();
        super.tearDown();
        for (i = 0; i < numiters; i++) {
            if (i % stepsize == 0) {
                runtime.gc();
                snapshot = runtime.freeMemory();
                log.info("hippo\t" + (i / stepsize) + "\t" + snapshot);
            }
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository(".");
            Session session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session.logout();
            repository.close();
        }
        runtime.gc();
        after = runtime.freeMemory();
        log.info("local1\t" + (i / stepsize) + "\t" + after);
    }

    @Ignore
    public void testLocal2() throws Exception {
        int i;
        Runtime runtime = Runtime.getRuntime();
        long snapshot, after, before = runtime.freeMemory();
        super.tearDown();
        for (i = 0; i < numiters; i++) {
            if (i % stepsize == 0) {
                runtime.gc();
                snapshot = runtime.freeMemory();
                log.info("hippo\t" + (i / stepsize) + "\t" + snapshot);
            }
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            Session session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session.logout();
            repository.close();
        }
        runtime.gc();
        after = runtime.freeMemory();
        log.info("local2\t" + (i / stepsize) + "\t" + after);
    }

    @Ignore
    public void testJackRabbit() throws Exception {
        int i;
        Runtime runtime = Runtime.getRuntime();
        long snapshot, after, before = runtime.freeMemory();
        //super.tearDown(true);
        for (i = 0; i < numiters; i++) {
            if (i % stepsize == 0) {
                runtime.gc();
                snapshot = runtime.freeMemory();
                log.info("jr\t" + (i / stepsize) + "\t" + snapshot);
            }
            JackrabbitRepository repository;
            repository = RepositoryImpl.create(RepositoryConfig.create(
                    getClass().getResourceAsStream("jackrabbit.xml"), "target"));
            Session session = repository.login(new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD));
            session.logout();
            repository.shutdown();
            Thread.sleep(1000);
        }
        runtime.gc();
        after = runtime.freeMemory();
        log.info("jr\t" + (i / stepsize) + "\t" + after);
    }

    @Test
    public void testDummy() {
        // dummy test to prevent failure due to no (enabled) tests in this
        // class.
    }
}
