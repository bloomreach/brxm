/*
 * Copyright 2014-2023 Bloomreach
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

import java.util.List;
import java.util.Random;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.onehippo.repository.monkey.MonkeyTest.log;

class Monkey {

    private final String name;
    private final Random random;
    private final Session session;
    private final List<Action> actions;

    Monkey(final String name, final Random random, final Session session, final List<Action> actions) {
        this.name = name;
        this.random = random;
        this.session = session;
        this.actions = actions;
    }

    boolean _do() {
        Action action = null;
        try {
            boolean executed = false;
            while (!executed) {
                action = selectAction();
                executed = executeAction(action);
            }
            log.info(name + " executed " + action.getName());
            return action instanceof SaveAction;
        } catch (RepositoryException e) {
            log.info(name + " failed to execute " + action.getName() + ": " + e);
        }
        return false;
    }

    boolean executeAction(Action action) throws RepositoryException {
        return action.execute(session);
    }

    String getName() {
        return name;
    }

    Session getSession() {
        return session;
    }

    private Action selectAction() {
        return actions.get(random.nextInt(actions.size()));
    }

}
