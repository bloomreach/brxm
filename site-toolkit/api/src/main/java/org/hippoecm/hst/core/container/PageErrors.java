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
package org.hippoecm.hst.core.container;

import java.util.Collection;

import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;

/**
 * PageErrors
 * <P>
 * Holds all error information in a page
 * </P>
 * 
 * @version $Id$
 */
public interface PageErrors {
    
    /**
     * Returns true if there's no component exceptions.
     * @return
     */
    public boolean isEmpty();
    
    /**
     * Returns collection of <CODE>HstComponentInfo</CODE> which causes exceptions.
     * @return
     */
    public Collection<HstComponentInfo> getComponentInfos();
    
    /**
     * Returns collection of <CODE>HstComponentException</CODE> caused by the specific component. 
     * @param componentInfo
     * @return
     */
    public Collection<HstComponentException> getComponentExceptions(HstComponentInfo componentInfo);
    
    /**
     * Returns all component exceptions.
     * @return
     */
    public Collection<HstComponentException> getAllComponentExceptions();
    
}
