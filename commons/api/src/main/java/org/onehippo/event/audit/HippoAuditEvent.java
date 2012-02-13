/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.event.audit;

/**
 * Audit event for Hippo.  Can be used in a fluent style to build and should be dispatched with the {@link
 * org.onehippo.event.HippoEventBus}.  Some categories have been pre-defined, but others can be used when necessary.
 */
public class HippoAuditEvent {

    public static final String CATEGORY_USER_MANAGEMENT = "user-management";
    public static final String CATEGORY_GROUP_MANAGEMENT = "group-management";
    public static final String CATEGORY_PERMISSIONS_MANAGEMENT = "permissions-management";
    public static final String CATEGORY_SECURITY = "security";

    private String action;
    private String application;
    private String category;
    private String user;
    private String result;
    private String message;

    public HippoAuditEvent(String application) {
        this.application = application;
        this.result = "success";
    }

    public String application() {
        return this.application;
    }

    public HippoAuditEvent user(String user) {
        this.user = user;
        return this;
    }

    public String user() {
        return user;
    }

    public HippoAuditEvent action(String action) {
        this.action = action;
        return this;
    }

    public String action() {
        return action;
    }

    public HippoAuditEvent category(String category) {
        this.category = category;
        return this;
    }

    public String category() {
        return category;
    }

    public HippoAuditEvent result(String result) {
        this.result = result;
        return this;
    }

    public String result() {
        return this.result;
    }

    public HippoAuditEvent message(String message) {
        this.message = message;
        return this;
    }

    public String message() {
        return this.message;
    }

}
