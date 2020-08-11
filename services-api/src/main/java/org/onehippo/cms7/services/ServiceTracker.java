/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services;

/**
 * A ServiceTracker interface implementation can be used to get notified when a service object is (un)registered
 * in a {@link WhiteboardServiceRegistry}.
 * @param <T> The type of the service object to be tracked
 */
public interface ServiceTracker<T> {
    /**
     * Invoked when a service object is registered.
     * @param serviceHolder the service object holder
     */
    void serviceRegistered(ServiceHolder<T> serviceHolder);

    /**
     * Invoked when a service object is unregistered.
     * @param serviceHolder the service object holder
     */
    void serviceUnregistered(ServiceHolder<T> serviceHolder);
}
