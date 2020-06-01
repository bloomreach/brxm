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
package org.hippoecm.hst.service;

/**
 * Factory util class to create lightweight JCR Node mapped POJO.
 * 
 * @version $Id$
 */
public class ServiceFactory {

    /**
     * Create and returns a lightweight JCR Node mapped POJO.
     * <P>
     * If the <CODE>proxyInterfacesOrDelegateeClass</CODE> argument is one-length array and
     * its own element is an instantiable delegatee class, then this method will instantiate the class after setting
     * a underlying {@link org.hippoecm.hst.service.Service} object.
     * </P> 
     * 
     * @param <T>
     * @param delegateeClass
     * @return proxy object or delegatee object
     * @throws Exception
     */
	public static <T> T create(Class... delegateeClass) throws Exception {
		T proxy = null;

		if (delegateeClass.length == 1 && !delegateeClass[0].isInterface()) {
			proxy = (T) delegateeClass[0].newInstance();
		}

		return proxy;
	}
   
}
