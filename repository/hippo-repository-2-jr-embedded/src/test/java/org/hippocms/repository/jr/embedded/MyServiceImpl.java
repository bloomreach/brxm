/*
 * Copyright 2007 Hippo.
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
package org.hippocms.repository.jr.embedded;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippocms.repository.jr.servicing.ServiceImpl;

public class MyServiceImpl extends ServiceImpl implements MyService {
    private boolean hasAction1;
    private boolean hasAction2;

    public MyServiceImpl() throws RemoteException {
        super();
    }

    public void doAction1() throws Exception, RemoteException {
        log.info("ACTION 1 CALLED");
        if (hasAction2 == true) {
            throw new Exception("action1 cannot be invoked when action2 has been performed");
        }
        hasAction1 = true;
    }

    public void doAction2() throws RemoteException {
        log.info("ACTION 2 CALLED");
        hasAction2 = true;
    }
}
