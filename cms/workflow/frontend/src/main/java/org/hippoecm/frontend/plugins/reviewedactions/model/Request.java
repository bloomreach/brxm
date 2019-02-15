/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class Request implements Serializable {

    public static final String INFO_REQUEST = "infoRequest";
    public static final String CANCEL_REQUEST = "cancelRequest";
    public static final String REJECT_REQUEST = "rejectRequest";
    public static final String ACCEPT_REQUEST = "acceptRequest";

    private final String id;
    private final Date schedule;
    private final String state;

    private final Boolean accept;
    private final Boolean reject;
    private final Boolean cancel;
    private final Boolean info;

    public Request(final String id, final Date schedule, final String state, final Map<String, ?> info) {
        this.id = id;
        this.schedule = schedule;
        this.state = state;

        this.accept = getTriState(info, ACCEPT_REQUEST);
        this.reject = getTriState(info, REJECT_REQUEST);
        this.cancel = getTriState(info, CANCEL_REQUEST);
        this.info = getTriState(info, INFO_REQUEST);
    }

    Boolean getTriState(Map<String, ?> info, String key) {
        if (info.containsKey(key) && info.get(key) instanceof Boolean) {
            return (Boolean) info.get(key);
        } else {
            return null;
        }
    }

    public String getId() {
        return id;
    }

    public Date getSchedule() {
        return schedule;
    }

    public String getState() {
        return state;
    }

    public Boolean getAccept() {
        return accept;
    }

    public Boolean getReject() {
        return reject;
    }

    public Boolean getCancel() {
        return cancel;
    }

    public Boolean getInfo() {
        return info;
    }
}

