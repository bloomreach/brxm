/*
 *  Copyright 2008-2023 Bloomreach
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
package org.hippoecm.hst.core.container;

/**
 * General valve interface.
 * Valves are to be assembled and invoked by a {@link Pipeline}.
 * 
 * @version $Id$
 */
public interface Valve
{
    
    /**
     * Invoke this valve
     * 
     * @param context
     * @throws ContainerException
     */
    void invoke(ValveContext context) throws ContainerException;

    /**
     * Initialize the valve before using in a pipeline.
     */
    void initialize() throws ContainerException;
    
    /**
     * Called by the container to indicate to a valve that the valve is being taken out of service
     */
    void destroy();
    
}