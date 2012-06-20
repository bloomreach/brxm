/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;

import org.hippoecm.repository.api.Workflow;

/**
 * This work-flow interface is made available by a special container document which is internal to the repository.  Each initiated
 * work-flow call is logged through this interface.  Work-flow calls that are the result of another work-flow call are not
 * automatically logged.  When a initiated work-flow call is successfully executed then the logEvent method is called.  Non
 * successful calls are not logged, and the log may be purged at times.  The log can be used for reporting, auditing is logged
 * through a plain log file.
 * 
 * This interface may be used to manually enter additional logging to the report log.
 */
public interface EventLoggerWorkflow extends Workflow {
    /**
     * @exclude
     */
    static final String SVN_ID = "$Id$";

    /**
     * Method that is invoked after a successful work-flow call.
     * @param who the user-id #javax.jcr.Session.getUserId() which originated the work-flow call
     * @param className the class that actually implements a work-flow step
     * @param methodName the work-flow method that was invoked
     * @throws RemoteException indicates that the work-flow call failed because of a connection problem with the repository
     */
    public void logEvent(String who, String className, String methodName) throws RemoteException;
}
