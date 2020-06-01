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
 * A ProxiedServiceTracker interface implementation can be used to get notified when a service is (un)registered in the
 * {@link HippoServiceRegistry} or a {@link WhiteboardProxiedServiceRegistry}
 * @param <T> The (main) service interface of the service to be tracked
 */
public interface ProxiedServiceTracker<T> {

    /**
     * Invoked when a service is registered.
     * @param serviceHolder the service holder
     */
    void serviceRegistered(ProxiedServiceHolder<T> serviceHolder);

    /**
     * Invoked when a service is unregistered.
     * @param serviceHolder the service holder
     */
    void serviceUnregistered(ProxiedServiceHolder<T> serviceHolder);
}
