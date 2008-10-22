/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.spi;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoQuery;

import org.hippoecm.repository.decorating.DecoratorFactory;

public class QueryDecorator extends org.hippoecm.repository.decorating.QueryDecorator {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public QueryDecorator(DecoratorFactory factory, Session session, Query query) {
        super(factory, session, query);
    }

    public Session getSession() {
        return session;
    }

    public String[] getArguments() {
        // FIXME
        return null;
    }

    public int getArgumentCount() {
        // FIXME
        return 0;
    }

    public QueryResult execute(Map<String,String> arguments) throws RepositoryException {
        return null;
    }

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {
        // FIXME
    }

    public void setLimit(long limit) throws RepositoryException {
        // FIXME
    }

    public void setOffset(long offset) throws RepositoryException {
        //FIXME
    }
}
