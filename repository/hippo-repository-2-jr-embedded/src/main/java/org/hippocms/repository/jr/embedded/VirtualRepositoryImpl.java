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
package org.hippocms.repository.jr.embedded;

import javax.jcr.Repository;
import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

public class VirtualRepositoryImpl
  implements Repository
{
  protected Repository actual;
  VirtualRepositoryImpl(Repository actual) {
    this.actual = actual;
  }
  public String[] getDescriptorKeys() {
    return actual.getDescriptorKeys();
  }
  public String getDescriptor(String key) {
    return actual.getDescriptor(key);
  }
  public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException
  {
    return new VirtualSessionImpl(actual.login(credentials, workspaceName), this);
  }
  public Session login(Credentials credentials) throws LoginException, RepositoryException {
    return new VirtualSessionImpl(actual.login(credentials), this);
  }
  public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
    return new VirtualSessionImpl(actual.login(workspaceName), this);
  }
  public Session login() throws LoginException, RepositoryException {
    return new VirtualSessionImpl(actual.login(), this);
  }
}
