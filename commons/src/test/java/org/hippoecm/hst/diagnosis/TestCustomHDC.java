/**
 * Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TestCustomHDC.
 */
public class TestCustomHDC {

    private static DemoExternalAPM demoExternalAPM;

    private Valve1 valve1 = new Valve1();
    private Component1 comp1 = new Component1();
    private Component2 comp2 = new Component2();
    private Query query1 = new Query();
    private Query query2 = new Query();

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty(HDC.class.getName(), CustomExternalApmIntegratedHDC.class.getName());

        demoExternalAPM = new DemoExternalAPM();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        System.clearProperty(HDC.class.getName());
    }

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

        // also the container will stop the root task.
        rootTask.stop();

        // clean up all the stored thread context information..
        HDC.cleanUp();

        // validates the metrics accumulated in the simulated external APM
        Map<String, List<Long>> metrics = demoExternalAPM.getMetrics();
        assertEquals(5, metrics.size()); // root, valve1, comp1, comp2 and query
        assertEquals(1, metrics.get("Custom/site_request-processing/duration").size());
        assertEquals(1, metrics.get("Custom/site_valve1/duration").size());
        assertEquals(1, metrics.get("Custom/site_comp1/duration").size());
        assertEquals(1, metrics.get("Custom/site_comp2/duration").size());
        assertEquals(2, metrics.get("Custom/site_query/duration").size());
    }

    private void sleepRandom(long max) {
        try {
            Thread.sleep(Math.abs(Math.round(Math.random() * max)));
        } catch (InterruptedException e) {
        }
    }

    /**
     * Simulated external APM which takes named metric and simply stores everything in memory.
     */
    public static class DemoExternalAPM {

        private Map<String, List<Long>> metrics = new LinkedHashMap<>();

        public void recordMetric(final String name, long value) {
            List<Long> metric = metrics.get(name);
            if (metric == null) {
                metric = new LinkedList<>();
                metrics.put(name, metric);
            }
            metric.add(value);
        }

        public Map<String, List<Long>> getMetrics() {
            return metrics;
        }
    }

    /**
     * Simulated custom APM integrated HDC implementation
     * which simply records a custom metric using an API.
     */
    public static class CustomExternalApmIntegratedHDC extends HDC {

        public CustomExternalApmIntegratedHDC() {
            super();
        }

        @Override
        protected Task doCreateTask(String name) {
            // Create a custom root task implementation.
            return new CustomTaskImpl(null, name) {
                @Override
                protected Task createSubtask(final Task parentTask, final String name) {
                    return new CustomTaskImpl(parentTask, name);
                }
            };
        }

        /**
         * Custom extension task implementation which records a metric on stop.
         */
        private class CustomTaskImpl extends DefaultTaskImpl {

            protected CustomTaskImpl(Task parentTask, String name) {
                super(parentTask, name);
            }

            @Override
            public void stop() {
                super.stop();
                //System.out.println("$$$$$ Custom/site_" + getName() + "/duration, " + getDurationTimeMillis());
                demoExternalAPM.recordMetric("Custom/site_" + getName() + "/duration", getDurationTimeMillis());
            }

            @Override
            protected Task createSubtask(final Task parentTask, final String name) {
                return new CustomTaskImpl(parentTask, name);
            }
        }
    }

    class Valve1 {
        public void execute() {
            sleepRandom(10);

            // A valve can also start a subtask from its current context task.
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
