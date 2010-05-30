/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository;

/**
 * This enumeration holds possible threshold categories why a javax.jcr.Session can
 * become overloaded.  When a session is active for a long time, or holds many changes
 * the performance may be damaged.
 */
public enum SessionStateThresholdEnum {
    /**
     * Specifies state that has explicitly been modified in the user session and is waiting to be persisted using a session.save().
     */
    UNPERSISTED,
    /**
     * Uncategorized state that is at least flushed when performing a session.refresh(false)
     */
    MISCELLANEOUS
}
