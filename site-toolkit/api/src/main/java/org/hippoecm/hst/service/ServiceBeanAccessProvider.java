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

import java.lang.reflect.Method;

/**
 * Property access provider interface to the {@link Service} instances.
 *
 * @deprecated since 2.28.05 (CMS 7.9.1). Do not use any more. No replacement
 */
@Deprecated
public interface ServiceBeanAccessProvider {
    
    Object getProperty(String namespacePrefix, String name, Class returnType, Method method);
    
    Object setProperty(String namespacePrefix, String name, Object value, Class returnType, Method method);
    
    Object invoke(String namespacePrefix, String name, Object [] args, Class returnType, Method method);
    
}
