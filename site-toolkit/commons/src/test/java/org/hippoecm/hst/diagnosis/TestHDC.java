/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.diagnosis;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import org.hippoecm.hst.util.TaskLogFormatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * TestHDC
 */
public class TestHDC {

    private static Logger log = LoggerFactory.getLogger(TestHDC.class);

    private Valve1 valve1 = new Valve1();
    private Valve2 valve2 = new Valve2();
    private Component1 comp1 = new Component1();
    private Component2 comp2 = new Component2();
    private Query query1 = new Query();
    private Query query2 = new Query();

    @Test
    public void testDefaultExample() throws Exception {
        // first, the HST container will start the root task first somewhere. e.g., HstFilter or InitializationValve
        Task rootTask = HDC.start("request-processing");

        // when invoking each valve, HST container can start a subtask
        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve1");
            valve1.execute();
            // if the container started a subtask, then it should stop the task.
            // in reality, it should use try ~ finally to guarantee this call.
            valveTask.stop();
        }

        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve2");
            valve2.execute();
            valveTask.stop();
        }

        // also the container will stop the root task.
        rootTask.stop();

        // all the task execution information can be collected and reported later (maybe in another valve before cleanupValve)
        logSummary();

        // clean up all the stored thread context information..
        HDC.cleanUp();
    }

    @Test
    public void testNOOPExample() throws Exception {

        // when invoking each valve, HST container can start a subtask
        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve1");
            valve1.execute();
            // if the container started a subtask, then it should stop the task.
            // in reality, it should use try ~ finally to guarantee this call.
            valveTask.stop();
        }

        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve2");
            valve2.execute();
            valveTask.stop();
        }

        // all the task execution information can be collected and reported later (maybe in another valve before cleanupValve)
        logSummary();

        // clean up all the stored thread context information..
        HDC.cleanUp();
    }

    @Test
    public void testRootTaskOnlyExample() throws Exception {
        // first, the HST container will start the root task first somewhere. e.g., HstFilter or InitializationValve
        Task rootTask = HDC.start("request-processing");
        HDC.setCurrentTask(HDC.NOOP_TASK);

        // when invoking each valve, HST container can start a subtask
        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve1");
            valve1.execute();
            // if the container started a subtask, then it should stop the task.
            // in reality, it should use try ~ finally to guarantee this call.
            valveTask.stop();
        }

        {
            Task valveTask = HDC.getCurrentTask().startSubtask("valve2");
            valve2.execute();
            valveTask.stop();
        }

        // also the container will stop the root task.
        rootTask.stop();

        // all the task execution information can be collected and reported later (maybe in another valve before cleanupValve)
        logSummary();

        // clean up all the stored thread context information..
        HDC.cleanUp();
    }

    private void logSummary() {
        Task rootTask = HDC.getRootTask();
        String formattedTask = TaskLogFormatter.getTaskLog(rootTask);
        assertTrue(formattedTask.length() > 0);
    }


    private void sleepRandom(long max) {
        try {
            Thread.sleep(Math.abs(Math.round(Math.random() * max)));
        } catch (InterruptedException e) {
        }
    }

    class Valve1 {
        public void execute() {
            sleepRandom(10);

            // A valve can also start a subtask from its current context task.
            Task compTask = HDC.getCurrentTask().startSubtask("comp1");
            Task compTaskA = HDC.getCurrentTask().startSubtask("comp1A");
            comp1.execute();
            compTaskA.stop();
            comp1.execute();
            compTask.stop();



            sleepRandom(10);

            compTask = HDC.getCurrentTask().startSubtask("comp2");
            comp2.execute();
            compTask.stop();

            sleepRandom(10);
        }
    }

    class Valve2 {
        public void execute() {
            sleepRandom(10);

            Task compTask = HDC.getCurrentTask().startSubtask("comp1");
            comp1.execute();
            compTask.stop();

            sleepRandom(10);

            compTask = HDC.getCurrentTask().startSubtask("comp2");
            comp2.execute();
            compTask.stop();

            sleepRandom(10);
        }
    }

    // Normally component developers do not need to manage tasks by themselves
    // because the task context for each component will be provided by the container.
    // e.g., by HstComponentInvoker or an AOP for HstComponentInvoker, etc.
    // so, the following component examples doesn't contain task management codes.
    class Component1 {
        public void execute() {
            sleepRandom(10);
            query1.execute("//element[jcr:contains(., 'hippo')]");
            sleepRandom(10);
        }
    }

    class Component2 {
        public void execute() {
            sleepRandom(10);
            query2.execute("//element[jcr:contains(., 'cms')]");
            sleepRandom(10);
        }
    }

    // HST Content Beans may manage its task context. e.g., HstQuery, HippoBeansIterator, etc.
    class Query {
        public void execute(String statement) {
            sleepRandom(10);
            Task queryTask = HDC.getCurrentTask().startSubtask("query");
            sleepRandom(10);
            queryTask.setAttribute("statement", statement);
            queryTask.stop();
            sleepRandom(10);
        }
    }

}
