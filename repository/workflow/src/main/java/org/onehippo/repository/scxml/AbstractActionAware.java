/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

/**
 * AbstractActionAware
 * <P>
 * An interface which can be implemented by non SCXML-based action classes
 * when they want to invoke methods of AbstractAction which started the delegation.
 * </P>
 */
public interface AbstractActionAware {

    /**
     * Sets the current AbstractAction instance.
     * @param action
     */
    public void setAbstractAction(AbstractAction action);

}
