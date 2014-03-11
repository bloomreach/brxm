/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.configuration.internal;

import java.util.Calendar;

/**
 * Provides methods to get information about the locked state of a hst configuration item.
 */
public interface ConfigurationLockInfo {

    /**
     * Returns the name of the user that has locked this configuration item or <code>null</code> if it is not locked.
     *
     * @return user name or <code>null</code>
     */
    String getLockedBy();

    /**
     * Returns the timestamp at which this configuration item became locked or <code>null</code> if it is not locked.
     *
     * @return locking timestamp or <code>null</code>
     */
    Calendar getLockedOn();
}
