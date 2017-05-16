/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.backend;

/**
 * ConfigurationCollisionException represents the event that an incoming update to a baseline configuration collides
 * with a runtime/repository change relative to the baseline configuration.
 *
 * TODO: currently, ConfigurationConfigService aborts on configuration conflicts.
 * TODO: Is that what we want, or do we want to support other/more behaviour?
 */
public class ConfigurationCollisionException extends Exception {

    ConfigurationCollisionException(final String message) {
        super(message);
    }
}
