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
package org.hippoecm.hst.logging;

/**
 * Wrapper interface to a LoggerFactory instance of HST Container.
 * <P>
 * By using this interface, a component can retrieve a {@link Logger} instance to 
 * leave logs in the container's logging context.
 * Also, some core components in hst-commons library cannot access to a specific logging
 * implementation like slf4j. For this reason, the components in hst-commons should get
 * {@Logger} instance from the container to leave a log message.
 * </P>
 * 
 * @version $Id$
 */
public interface LoggerFactory {
    
    /**
     * Returns HST Logger with the category name
     * @param name
     * @return
     */
    Logger getLogger(String name);
    
    /**
     * Returns HST Logger with the category name and wrapper logger fqcn used to find logging location
     * @param name
     * @param fqcn
     * @return
     */
    Logger getLogger(String name, String fqcn);
    
}
