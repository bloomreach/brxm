/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.security;

import java.util.Set;

/**
 * SessionUser wrapping another SessionUser for SessionDelegation, having a "," concatenated {@link #getId()} and a merged
 * {@link #getMemberships()}.
 * <p>
 * The underlying (delegate) {@link SessionUser} can be retrieved through {@link #getDelegateUser()}.
 * </p>
 * <p>
 * A set of the original individual user ids can be retrieved through {@link #getIds()}.
 * </p>
 * <p>
 * Both {@link #isSystemUser()} and {@link #isActive()} will always return false because this does not represent an
 * actual repository user.
 * </p>
 * <p>
 * All other {@link User} (not the {@link SessionUser} methods are overridden to always return null.
 * </p>
 */
public interface SessionDelegateUser extends SessionUser {
    Set<String> getIds();
    SessionUser getDelegateUser();
}
