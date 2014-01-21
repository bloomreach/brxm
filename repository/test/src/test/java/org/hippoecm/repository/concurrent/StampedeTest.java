/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.concurrent.TimeUnit;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.onehippo.repository.concurrent.ActionRunner;
import org.onehippo.repository.concurrent.action.Action;
import org.onehippo.repository.concurrent.action.ActionContext;
import org.onehippo.repository.concurrent.action.ActionFailure;
import org.onehippo.repository.concurrent.action.AddAssetAction;
import org.onehippo.repository.concurrent.action.AddAssetFolderAction;
import org.onehippo.repository.concurrent.action.AddDocumentFolderAction;
import org.onehippo.repository.concurrent.action.AddDocumentLinkAction;
import org.onehippo.repository.concurrent.action.AddNewsDocumentAction;
import org.onehippo.repository.concurrent.action.DeleteAssetAction;
import org.onehippo.repository.concurrent.action.DeleteDocumentAction;
import org.onehippo.repository.concurrent.action.DeleteFolderAction;
import org.onehippo.repository.concurrent.action.DepublishAction;
import org.onehippo.repository.concurrent.action.EditDocumentAction;
import org.onehippo.repository.concurrent.action.JanitorAction;
import org.onehippo.repository.concurrent.action.LoadDocumentAction;
import org.onehippo.repository.concurrent.action.MoveDocumentAction;
import org.onehippo.repository.concurrent.action.PublishAction;
import org.onehippo.repository.concurrent.action.RenameAssetAction;
import org.onehippo.repository.concurrent.action.RenameDocumentAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.fail;

/**
 * Run a number of parallel threads that hit the repository with random actions.
 * You can tweak the number of threads, the maximum number of steps and the maximum
 * running time of the test setting the system properties StampedeTest.THREADS, StampedeTest.STEPS,
 * and StampedeTest.TIMEOUT respectively.
 */
@RunWith(value = Parameterized.class)
public class StampedeTest {

    private static final Logger log = LoggerFactory.getLogger("org.hippoecm.repository.concurrent.stampede");

    private static final List<Class<? extends Action>> actions = new ArrayList<Class<? extends Action>>() {{
        add(AddDocumentFolderAction.class);
        add(AddAssetFolderAction.class);
        add(AddNewsDocumentAction.class);
        add(PublishAction.class);
        add(DepublishAction.class);
        add(DeleteDocumentAction.class);
        add(LoadDocumentAction.class);
        add(AddDocumentLinkAction.class);
        add(EditDocumentAction.class);
        add(AddAssetAction.class);
        add(DeleteAssetAction.class);
        add(RenameAssetAction.class);
        add(DeleteFolderAction.class);
        add(RenameDocumentAction.class);
        add(MoveDocumentAction.class);
        add(JanitorAction.class);
//        add(LockAction.class);
    }};

    private HippoRepository hippoRepository;

    private final long duration;
    private final int nthreads;
    private final int throttle;
    private final boolean prompt;

    public StampedeTest(int minutes, int nthreads, int throttle, boolean prompt) {
        this.duration = TimeUnit.MINUTES.toMillis(minutes);
        this.nthreads = nthreads;
        this.throttle = throttle;
        this.prompt = prompt;

    }

    @Parameters
    public static Collection<Object[]> parameters() {
        Object[][] data = new Object[][] {{
                Integer.getInteger("stampede.minutes", 1),
                Integer.getInteger("stampede.threads", 3),
                Integer.getInteger("stampede.throttle", 0),
                Boolean.getBoolean("stampede.prompt")}};
        return Arrays.asList(data);
    }

    @Before
    public void setUp() throws Exception {
        hippoRepository = HippoRepositoryFactory.getHippoRepository();
        final Repository repository = hippoRepository.getRepository();
        final Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        new ActionContext(session, log).start();
        session.logout();
    }

    @After
    public void tearDown() throws Exception {
        if (hippoRepository != null) {
            if (prompt) {
                System.out.println("Press enter to stop repository...");
                System.console().readLine();
            }
            hippoRepository.close();
        }
    }

    @Test(timeout=3600000)
    public void stampede() throws Exception {
        final ActionRunner[] runners = new ActionRunner[nthreads];
        for (int i = 0; i < nthreads; i++) {
            Session runnerSession = hippoRepository.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
            runners[i] = new ActionRunner(new ActionContext(runnerSession, log), actions, duration, throttle);
            runners[i].initialize();
            runners[i].setDaemon(true);
        }
        if (prompt) {
            System.out.println("Press enter to start test...");
            System.console().readLine();
        }
        for(ActionRunner runner : runners) {
            runner.start();
        }
        long startTime;
        startTime = System.currentTimeMillis();
        for(ActionRunner runner : runners) {
            runner.join();
        }
        long endTime = System.currentTimeMillis();
        for(ActionRunner runner : runners) {
            runner.terminate();
            runner.getContext().stop();
        }
        if(!report(runners, startTime, endTime)) {
            fail();
        }
    }

    public boolean report(ActionRunner[] runners, long startTime, long endTime) {
        int totalSteps = 0;
        int totalWriteSteps = 0;
        for (Class<? extends Action> actionClass : actions) {

            int actionCount = 0;
            int actionFailures = 0;
            int actionMissed = 0;
            int timeSpent = 0;
            for (ActionRunner runner : runners) {
                final Action action = runner.getContext().getAction(actionClass);
                totalSteps += action.getCount();
                if (action.isWriteAction()) {
                    totalWriteSteps += action.getCount();
                }
                for (ActionFailure failure : runner.getFailures()) {
                    if (failure.getAction().getClass().equals(actionClass)) {
                        actionFailures++;
                    }
                }
                actionCount += action.getCount();
                actionMissed += action.getMissed();
                timeSpent += action.getTimeSpent();
            }

            int averageTime = (actionCount == 0 || timeSpent == 0) ? 0 : (timeSpent / actionCount);
            System.err.println(actionClass.getSimpleName() + " executed " + actionCount + " times; " +
                    "missed "+ actionMissed + " failed " + actionFailures + " times; " +
                    "total time spent " + timeSpent + " ms; " +
                    "average time " + averageTime);
        }
        int totalFailures = 0, totalSuccesses = 0, totalMisses = 0;
        List<ActionFailure> failures = new ArrayList<ActionFailure>();
        for (ActionRunner runner : runners) {
            totalFailures += runner.getFailures().size();
            failures.addAll(runner.getFailures());
            totalSuccesses += runner.getSuccesses();
            totalMisses += runner.getMisses();
        }
        System.err.println("Total actions run: " + totalSteps);
        System.err.println("Total successful actions: " + totalSuccesses);
        System.err.println("Total write actions run: " + totalWriteSteps);
        // missed actions are those that have recoverable exceptions; added for completeness
        System.err.println("Total missed actions: " + totalMisses);
        System.err.println("Total failed actions: " + totalFailures);
        System.err.println("Successrate: " + (totalSuccesses + totalMisses) / (double)totalSteps);
        System.err.println("Total time it took: " + (endTime - startTime) / 1000.0 + " sec");

        if (totalFailures > 0) {
            System.err.println("Failures:");
            for (ActionFailure failure : failures) {
                failure.printFailure();
            }
            return false;
        }
        return true;
    }

}
