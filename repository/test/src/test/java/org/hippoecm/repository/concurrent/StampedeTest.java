/*
 *  Copyright 2011,2012 Hippo.
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
package org.hippoecm.repository.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.concurrent.action.Action;
import org.hippoecm.repository.concurrent.action.AddAssetAction;
import org.hippoecm.repository.concurrent.action.AddAssetFolderAction;
import org.hippoecm.repository.concurrent.action.AddDocumentFolderAction;
import org.hippoecm.repository.concurrent.action.AddDocumentLinkAction;
import org.hippoecm.repository.concurrent.action.AddNewsDocumentAction;
import org.hippoecm.repository.concurrent.action.DeleteAssetAction;
import org.hippoecm.repository.concurrent.action.DeleteDocumentAction;
import org.hippoecm.repository.concurrent.action.DeleteFolderAction;
import org.hippoecm.repository.concurrent.action.DepublishAction;
import org.hippoecm.repository.concurrent.action.EditDocumentAction;
import org.hippoecm.repository.concurrent.action.GetAssetBaseNodeAction;
import org.hippoecm.repository.concurrent.action.GetDocumentBaseNodeAction;
import org.hippoecm.repository.concurrent.action.GetRandomChildNodeAction;
import org.hippoecm.repository.concurrent.action.LoadDocumentAction;
import org.hippoecm.repository.concurrent.action.MoveDocumentAction;
import org.hippoecm.repository.concurrent.action.PublishAction;
import org.hippoecm.repository.concurrent.action.RenameAssetAction;
import org.hippoecm.repository.concurrent.action.RenameDocumentAction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.fail;

/**
 * Run a number of parallel threads that hit the repository with random actions.
 * You can tweak the number of threads, the maximum number of steps and the maximum
 * running time of the test setting the system properties StampedeTest.THREADS, StampedeTest.STEPS,
 * and StampedeTest.TIMEOUT respectively.
 */
@RunWith(value = Parameterized.class)
public class StampedeTest extends AbstractRandomActionTest {

    private final List<Action> actionSet = new ArrayList<Action>();

    public StampedeTest(int minutes, int nthreads) {
        super(minutes, TimeUnit.MINUTES, nthreads);
    }

    @Parameters
    public static Collection<Object[]> parameters() {
        Object[][] data = new Object[][] {{ Integer.getInteger("stampede.minutes", 1), Integer.getInteger("stampede.threads", 3) }};
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        actionSet.add(new GetDocumentBaseNodeAction(context));
        actionSet.add(new GetAssetBaseNodeAction(context));
        actionSet.add(new GetRandomChildNodeAction());
        actionSet.add(new AddDocumentFolderAction(context));
        actionSet.add(new AddAssetFolderAction(context));
        actionSet.add(new AddNewsDocumentAction(context));
        actionSet.add(new PublishAction());
        actionSet.add(new DepublishAction());
        actionSet.add(new DeleteDocumentAction());
        actionSet.add(new LoadDocumentAction());
        actionSet.add(new AddDocumentLinkAction(context));
        actionSet.add(new EditDocumentAction());
        actionSet.add(new AddAssetAction(context));
        actionSet.add(new DeleteAssetAction());
        actionSet.add(new RenameAssetAction());
        actionSet.add(new DeleteFolderAction(context));
        actionSet.add(new RenameDocumentAction());
        actionSet.add(new MoveDocumentAction(context));
//        actionSet.add(new LockAction());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test(timeout=3600000)
    public void stampede() throws Exception {
        final RandomActionRunner[] runners = new RandomActionRunner[nthreads];
        for (int i = 0; i < nthreads; i++) {
            Session runnerSession = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
            runners[i] = new RandomActionRunner(runnerSession, actionSet);
            runners[i].setDaemon(true);
        }
        if (Boolean.getBoolean("stampede.prompt")) {
            System.out.println("Press enter to start test...");
            System.console().readLine();
        }
        for(RandomActionRunner runner : runners) {
            runner.start();
        }
        long startTime;
        startTime = System.currentTimeMillis();
        for(RandomActionRunner runner : runners) {
            runner.join();
        }
        long endTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        for(RandomActionRunner runner : runners) {
            runner.terminate(executor);
        }
        if(!report(runners, startTime, endTime, actionSet)) {
            fail();
        }
    }
}
