/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.rest;

import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;

/**
 * @version "$Id$"
 */
public class ErrorMessageRestful extends MessageRestful {

    private static final long serialVersionUID = 1L;


    public ErrorMessageRestful() {

    }

    public ErrorMessageRestful(final String value) {
        super(value);
    }

    public ErrorMessageRestful(final String message, final DisplayEvent.DisplayType displayType) {
        super(message, displayType);
    }

    @Override
    public boolean isSuccessMessage() {
        return false;
    }
}
