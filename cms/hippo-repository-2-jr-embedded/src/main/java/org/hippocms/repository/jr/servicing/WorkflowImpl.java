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
package org.hippocms.repository.jr.servicing;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public class WorkflowImpl
  implements Workflow
{
  boolean hasAction1 = false;
  boolean hasAction2 = false;
  public WorkflowImpl() {
  }
  public void doAction1() throws Exception, RemoteException {
    if(hasAction2 == true) {
      throw new Exception("action1 cannot be invoked when action2 has been performed");
    }
    hasAction1 = true;
  }
  public void doAction2() throws RemoteException {
    hasAction2 = true;
  }

  /* FIXME: following functions should no longer be required in future. */
  public void setAction1(boolean action1) {
    hasAction1 = action1;
  }
  public boolean getAction1() {
    return hasAction1;
  }
  public void setAction2(boolean action2) {
    hasAction2 = action2;
  }
  public boolean getAction2() {
    return hasAction2;
  }
}
