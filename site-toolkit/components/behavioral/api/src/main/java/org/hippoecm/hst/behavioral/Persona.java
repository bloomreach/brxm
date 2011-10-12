/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral;

/**
 * A Persona represents a set of users that match certain criteria. These criteria are expressed
 * by an {@link Expression}.
 */
public interface Persona {

    /**
     * @return  the identifier of this persona
     */
    String getId();
    
    /**
     * @return  the human readable name of this persona
     */
    String getName();
    
    /**
     * @return  the {@link Expression} that represents the criteria in order that a user be assigned this persona
     */
    Expression getExpression();

}
