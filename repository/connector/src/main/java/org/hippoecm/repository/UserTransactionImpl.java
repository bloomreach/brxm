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
package org.hippoecm.repository;

import javax.jcr.Session;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;

import org.apache.jackrabbit.api.XASession;

/**
 * A wrapper class around the TransactionManager that implements
 * UserTransaction. The main purpose is to automatically enlist and
 * delist the XASession resource.
 */
public class UserTransactionImpl implements UserTransaction {

    /**
     * SVN ID
     */

    /**
     * XAResource
     */
    private XAResource XARes;

    /**
     * External provided transactionmanager
     */
    private TransactionManager tm;

    /**
     * Create a new instance of this class. Takes a session as parameter.
     * @param session session. If session is not of type
     * {@link XASession}, an <code>IllegalArgumentException</code>
     * is thrown
     */
    public UserTransactionImpl(TransactionManager tm, Session session) {
        this.tm = tm;
        if (session instanceof XASession) {
            XARes = ((XASession) session).getXAResource();
        } else if(session instanceof XAResource) {
            XARes = (XAResource) session;
        } else {
            throw new IllegalArgumentException("Session not of type XASession");
        }
    }

    /**
     * @see javax.transaction.UserTransaction#begin
     */
    public void begin() throws NotSupportedException, IllegalStateException, SystemException {
        try {
            tm.begin();
            tm.getTransaction().enlistResource(XARes);
        } catch (RollbackException e) {
            // TODO Do something useful with a rollback exception?
            e.printStackTrace();
        }
    }

    /**
     * @see javax.transaction.UserTransaction#commit
     */
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
            RollbackException, SecurityException, SystemException {
        int flag = XAResource.TMSUCCESS;
        tm.getTransaction().delistResource(XARes, flag);
        tm.commit();
    }

    /**
     * @see javax.transaction.UserTransaction#getStatus
     */
    public int getStatus() throws SystemException {
        return tm.getStatus();
    }

    /**
     * @see javax.transaction.UserTransaction#rollback
     */
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        int flag = XAResource.TMFAIL;
        tm.getTransaction().delistResource(XARes, flag);
        tm.rollback();
    }

    /**
     * @see javax.transaction.UserTransaction#setRollbackOnly
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        tm.setRollbackOnly();
    }

    /**
     * @see javax.transaction.UserTransaction#setTransactionTimeout
     */
    public void setTransactionTimeout(int timeout) throws SystemException {
        tm.setTransactionTimeout(timeout);
    }

}
