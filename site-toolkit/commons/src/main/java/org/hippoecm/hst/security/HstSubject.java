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
package org.hippoecm.hst.security;

import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

/**
 * Wrapper for the javax.security.auth.Subject class.
 * Due to a design oversight in JAAS 1.0, the javax.security.auth.Subject.getSubject method does not return the Subject 
 * that is associated with the running thread !inside! a java.security.AccessController.doPrivileged code block.
 * As a result, the current subject cannot be determined correctly.
 * This class uses the ThreadLocal mechanism to carry the thread-specific instance of the subject 
 * 
 * @version $Id$
 */
public class HstSubject {

    private static ThreadLocal<Subject> tlSubject = new ThreadLocal<Subject>();
    
    private HstSubject() {
        
    }
    
    /**
     * Get the <code>Subject</code> associated with the provided
     * <code>AccessControlContext</code> fromn the current Thread or from the standard SUBJECT mechansim 
     */
    public static Subject getSubject(final AccessControlContext acc) {
        Subject subject = (Subject) tlSubject.get();
        
        if (subject == null) {
            subject = Subject.getSubject(acc);
        }
        
        return subject;
    }

    /**
     * Perform work as a particular <code>Subject</code> after setting subject reference in current thread 
     */
    public static <T> T doAs(final Subject subjectInput, final PrivilegedAction<T> action) {
        Subject subject = subjectInput;
        
        if (subject == null) {
            subject = HstSubject.getSubject(null);
        }
        
        tlSubject.set(subject);
        
        return (T)Subject.doAs(subject, action);
    }

    /**
     * Perform work as a particular <code>Subject</code> after setting subject reference in current thread.
     */
    public static <T> T doAs(final Subject subjectInput, final PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        Subject subject = subjectInput;
        
        if (subject == null) {
            subject = HstSubject.getSubject(null);
        }
        
        tlSubject.set(subject);
        
        return (T)Subject.doAs(subject, action);
    }

    /**
     * Perform privileged work as a particular <code>Subject</code> after setting subject reference in current thread.
     */
    public static <T> T doAsPrivileged(final Subject subjectInput, final PrivilegedAction<T> action, final AccessControlContext acc) {
        Subject subject = subjectInput;
        
        if (subject == null) {
            subject = HstSubject.getSubject(acc);
        }
        
        tlSubject.set(subject);
        
        return (T)Subject.doAsPrivileged(subject, action, acc);
    }

    /**
     * Perform privileged work as a particular <code>Subject</code> after setting subject reference in current thread.
     */
    public static <T> T doAsPrivileged(final Subject subjectInput, final PrivilegedExceptionAction<T> action, final AccessControlContext acc) throws PrivilegedActionException {
        Subject subject = subjectInput;
        
        if (subject == null) {
            subject = HstSubject.getSubject(acc);
        }
        
        tlSubject.set(subject);
        
        return (T)Subject.doAsPrivileged(subject, action, acc);
    }

    /**
     * Clear subject reference in current thread.
     */
    public static void clearSubject() {
        tlSubject.remove();
    }
}
