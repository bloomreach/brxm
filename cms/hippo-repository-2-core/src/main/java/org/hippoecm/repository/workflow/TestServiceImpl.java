/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
package org.hippoecm.repository.workflow;

import java.rmi.RemoteException;

import org.hippoecm.repository.jr.servicing.ServiceImpl;

public class TestServiceImpl extends ServiceImpl implements TestService {
    private String uuid;
    public boolean hasAction1;
    private boolean hasAction2;
    public TestServiceObject object;

    public TestServiceImpl() throws RemoteException {
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

    public void setMyContent(String content) {
        if (object == null)
            object = new TestServiceObject();
        object.setMyContent(content);
    }

    public String getMyContent() {
        if (object != null)
            return object.getMyContent();
        else
            return null;
    }

    public boolean hasAction(int which) {
        switch (which) {
        case 1:
            return hasAction1;
        case 2:
            return hasAction2;
        default:
            return false;
        }
    }
}
