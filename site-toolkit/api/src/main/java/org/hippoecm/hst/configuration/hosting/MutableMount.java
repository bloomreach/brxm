/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.hosting;

import java.util.Calendar;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.service.ServiceException;

/**
 * Mount extension that decouples channel info retrieval from the mount service construction.
 * It is only safe to use the methods that are exposed while the mount service is built;
 * i.e. with the HstManagerImpl monitor held.
 */
public interface MutableMount extends Mount {

    /**
     * Set the channel info for the mount.  The info must be constant,
     * i.e. it must always return the same values.
     * @param info
     */
    void setChannelInfo(ChannelInfo info);

    /**
     * 
     * @param mount the {@link MutableMount} to add
     * @throws IllegalArgumentException if the <code>mount</code> could not be added
     * @throws ServiceException if the <code>mount</code> could not be added
     */
    void addMount(MutableMount mount) throws IllegalArgumentException, ServiceException;
    
    /**
     * @return the cms location (fully qualified URL) or <code>null</code> if not configured
     */
    String getCmsLocation();

    /**
     * Return null when the mount is not locked.
     *
     * @return userId of the lock owner or null if not locked
     */
    String getLockedBy();

    /**
     * Set the mount's lock owner userId.
     */
    void setLockedBy(String userId);

    /**
     * Get the date on which the mount was locked. If the mount is not locked, this date can by any date and null.
     *
     * @return the date on which the mount was locked
     */
    Calendar getLockedOn();

    /**
     * Set the date on which the mount was locked.
     *
     * @param lockedOn
     */
    void setLockedOn(Calendar lockedOn);

}
