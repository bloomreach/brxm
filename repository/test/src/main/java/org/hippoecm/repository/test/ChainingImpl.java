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
package org.hippoecm.repository.test;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Vector;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowImpl;

public class ChainingImpl extends WorkflowImpl implements Chaining {

    static Vector<String> result = new Vector<String>();

    Document self;

    public ChainingImpl() throws RemoteException {
    }

    public void test() throws WorkflowException, MappingException {
        try {
            Chaining workflow = (Chaining) getWorkflowContext().getWorkflow("test", self);
            workflow.test1();
            workflow.test3();
        } catch(RemoteException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new WorkflowException("recursive invocation failed");
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new WorkflowException("recursive invocation failed");
        }
    }

    public void test1() throws WorkflowException, RepositoryException {
        result.add("1");
        test2();
        test4();
    }

    public void test2() throws WorkflowException, RepositoryException {
        result.add("2");
        test5();
    }

    public void test3() throws WorkflowException, RepositoryException {
        result.add("5");
        test6();
    }

    public void test4() throws WorkflowException, RepositoryException {
        result.add("4");
    }

    public void test5() throws WorkflowException, RepositoryException {
        result.add("3");
    }

    public void test6() throws WorkflowException, RepositoryException {
        result.add("6");
    }

    public void schedule(Date future) throws WorkflowException, RepositoryException {
        WorkflowContext context = getWorkflowContext().getWorkflowContext(future);
        Chaining workflow = (Chaining) context.getWorkflow("test");
        try {
            workflow.later();
        } catch(RemoteException ex) {
            throw new WorkflowException(ex.getMessage(), ex);
        }
    }

    public void later() throws WorkflowException, RepositoryException {
        result.add("done");
    }
}
