/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.monkey;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.clustering.ClusterTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static junit.framework.Assert.fail;

/**
 * Asserts consistency across multiple random concurrency scenarios
 * both within a single cluster node and across cluster nodes.
 * <p>
 *     The system property -Dorg.onehippo.repository.monkey.MonkeyTest.actionCount
 *     controls the amount of action to perform during each test. Defaults to 1000.
 * </p>
 */
public class MonkeyTest extends ClusterTest {

    static final Logger log = LoggerFactory.getLogger(MonkeyTest.class);

    private static final int actionCount = Integer.getInteger(MonkeyTest.class.getName() + ".actionCount", 1000);

    private Random random;
    private List<Action> actions;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        random = new Random();
        actions = Arrays.asList(
                new AddNodeAction("a"), new AddNodeAction("b"), new AddNodeAction("a/b"),
                new RemoveNodeAction("a"), new RemoveNodeAction("b"), new RemoveNodeAction("a/b"),
                new MoveNodeAction("b", "a/b"), new MoveNodeAction("a/b", "b/a"), new MoveNodeAction("a", "b"),
                new SetPropertyAction("a/p"), new SetPropertyAction("a/b/p"),
                new RemovePropertyAction("a/p"), new RemovePropertyAction("a/b/p"),
                new SaveAction()
        );
    }

    @Test
    public void monkeyCluster() throws Exception {
        Monkey monkey1 = createMonkey("monkey1", repo1);
        Monkey monkey2 = createMonkey("monkey2", repo2);
        boolean saved = false;
        int count = 0;
        while ((!saved || checkClusterConsistency()) && count < actionCount) {
            saved = selectMonkey(monkey1, monkey2)._do();
            Thread.sleep(10);
            count += 1;
        }
        if (count < actionCount) {
            // todo: determine failure sequence
            fail("Detected inconsistency after executing " + count + " actions.");
        }
    }

    @Test
    public void monkeySingleNode() throws Exception {
        Monkey monkey1 = createMonkey("monkey1", repo1);
        Monkey monkey2 = createMonkey("monkey2", repo1);
        boolean saved = false;
        int count = 0;
        while ((!saved || checkSingleNodeConsistency(repo1)) && count < actionCount) {
            saved = selectMonkey(monkey1, monkey2)._do();
            Thread.sleep(10);
            count += 1;
        }
        if (count < actionCount) {
            // todo: determine failure sequence
            fail("Detected inconsistency after executing " + count + " actions.");
        }
    }

    private Monkey createMonkey(final String name, Object repo) throws RepositoryException {
        final Session session = loginSession(repo);
        return new Monkey(name, session, actions);
    }

    private Monkey selectMonkey(final Monkey monkey1, final Monkey monkey2) {
        if (random.nextBoolean()) {
            return monkey1;
        }
        return monkey2;
    }

    private boolean checkClusterConsistency() throws RepositoryException, IOException {
        log.info("checking cluster consistency...");
        return clusterContentEqual()
                && checkIndexConsistency(repo1)
                && checkIndexConsistency(repo2)
                && checkDatabaseConsistency(repo1)
                && checkDatabaseConsistency(repo2);
    }

    private boolean checkSingleNodeConsistency(Object repo) throws IOException, RepositoryException {
        log.info("checking node consistency...");
        return checkIndexConsistency(repo) && checkDatabaseConsistency(repo);
    }

}
