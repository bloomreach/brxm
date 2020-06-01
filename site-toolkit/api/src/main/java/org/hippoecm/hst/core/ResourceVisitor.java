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
package org.hippoecm.hst.core;

/**
 * ResourceVisitor
 * <P>
 * Generic resource visitor interface for resource traversals.
 * </P>
 * 
 * @version $Id$
 */
public interface ResourceVisitor {
    
    /**
     * Value to return to continue a traversal.
     */
    Object CONTINUE_TRAVERSAL = null;

    /**
     * A generic value to return to stop a traversal.
     */
    Object STOP_TRAVERSAL = new Object();

    /**
     * Called at each resource in a traversal.
     * 
     * @param resource
     *            The resource
     * @return CONTINUE_TRAVERSAL (null) if the traversal should continue, or a non-null return
     *         value for the traversal method if it should stop. If no return value is useful,
     *         the generic non-null value STOP_TRAVERSAL can be used.
     */
    Object resource(Object resource);
    
}
