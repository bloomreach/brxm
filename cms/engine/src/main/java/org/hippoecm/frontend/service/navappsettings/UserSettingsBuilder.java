/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.service.navappsettings;

import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.frontend.service.UserSettings;

final class UserSettingsBuilder {

    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String language;
    private TimeZone timeZone;

    UserSettingsBuilder userName(String userName) {
        this.userName = userName;
        return this;
    }

    UserSettingsBuilder firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    UserSettingsBuilder lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    UserSettingsBuilder email(String email) {
        this.email = email;
        return this;
    }

    UserSettingsBuilder language(String language) {
        this.language = language;
        return this;
    }

    UserSettingsBuilder timeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    UserSettings build() {
        final String name = getUserName();
        return new UserSettings() {

            @Override
            public String getUserName() {
                return name;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getLanguage() {
                return language;
            }

            @Override
            public TimeZone getTimeZone() {
                return timeZone;
            }
        };
    }

    private String getUserName() {
        if (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(lastName)) {
            return String.format("%s %s",
                    Optional.ofNullable(firstName).orElse(""),
                    Optional.ofNullable(lastName).orElse("")
            ).trim();
        }
        return userName;
    }
}
