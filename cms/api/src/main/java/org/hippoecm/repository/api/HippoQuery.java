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
package org.hippoecm.repository.api;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

public interface HippoQuery extends Query {
    public static final String HIPPOQL = "HIPPOQL";

    public String[] getArguments() throws RepositoryException;

    public int getArgumentCount() throws RepositoryException;

    public QueryResult execute(String argument) throws RepositoryException;

    public QueryResult execute(String[] arguments) throws RepositoryException;

    public QueryResult execute(Map<String,String> arguments) throws RepositoryException;

    public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException;

    /* FIXME
     * The following methods are part of JCR2, and should be supported
     * as well within the Query and the QueryManager class:
     * 
     * public void Query.setLimit(long limit);
     * public void Query.setOffset(long offset);
     * public PreparedQuery QueryManager.createPreparedQuery(String statement, String language) throws InvalidQueryException, RepositoryException;
     */
}
