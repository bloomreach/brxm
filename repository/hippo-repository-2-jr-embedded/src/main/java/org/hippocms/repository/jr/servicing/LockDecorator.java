/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippocms.repository.jr.servicing;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 *
 */
public class LockDecorator extends AbstractDecorator implements Lock {

    protected final Lock lock;

    public LockDecorator(DecoratorFactory factory, Session session, Lock lock) {
        super(factory, session);
        this.lock = lock;
    }

    /**
     * @inheritDoc
     */
    public Node getNode() {
        return factory.getNodeDecorator(session, lock.getNode());
    }

    /**
     * @inheritDoc
     */
    public String getLockOwner() {
        return lock.getLockOwner();
    }

    /**
     * @inheritDoc
     */
    public boolean isDeep() {
        return lock.isDeep();
    }

    /**
     * @inheritDoc
     */
    public String getLockToken() {
        return lock.getLockToken();
    }

    /**
     * @inheritDoc
     */
    public boolean isLive() throws RepositoryException {
        return lock.isLive();
    }

    /**
     * @inheritDoc
     */
    public boolean isSessionScoped() {
        return lock.isSessionScoped();
    }

    /**
     * @inheritDoc
     */
    public void refresh() throws LockException, RepositoryException {
        lock.refresh();
    }
}
