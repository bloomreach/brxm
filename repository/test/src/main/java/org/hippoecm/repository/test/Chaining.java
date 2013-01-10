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
package org.hippoecm.repository.test;

import java.util.Date;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;

public interface Chaining extends Workflow {

    public void test() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test1() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test2() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test3() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test4() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test5() throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void test6() throws WorkflowException, MappingException, RepositoryException, RemoteException;

    public void schedule(Date future) throws WorkflowException, MappingException, RepositoryException, RemoteException;
    public void later() throws WorkflowException, MappingException, RepositoryException, RemoteException;
}

