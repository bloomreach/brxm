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
package org.hippoecm.hst.util;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * ClasspathResourceScanner
 * <P>
 * An implementation of this interface is responsible for collecting classpath resources. 
 * </P>
 * @version $Id$
 */
public interface ClasspathResourceScanner {
    
    /**
     * Scans classpath from the location specified by <CODE>locationPatterns</CODE> 
     * for all class names which annotates the speified <CODE>annotationType</CODE>.
     * <P>
     * Note: An implementation is recommended to support <CODE>locationPattern</CODE> with <CODE>classpath*:</CODE> prefix at least
     * like the following example:
     * <UL>
     * <LI>classpath*:org/examples/beans/**&#x2F;*.class : For all matching resources from the class path, which are descendants under org/examples/beans/ package.</LI>
     * <LI>classpath*:org/examples/beans/*.class : For all matching resources from the class path, which are one level children under org/examples/beans/ package.</LI>
     * </UL>
     * </P>
     * @param annotationType
     * @param matchSuperClass
     * @param locationPatterns
     * @return
     */
    public Set<String> scanClassNamesAnnotatedBy(Class<? extends Annotation> annotationType, boolean matchSuperClass, String ... locationPatterns);
    
}
