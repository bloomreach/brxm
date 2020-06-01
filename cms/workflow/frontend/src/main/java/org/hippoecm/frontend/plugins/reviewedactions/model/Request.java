/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.util.collections.MiniMap;

public class Request implements Serializable {

    private final String id;
    private final Date schedule;
    private final String state;

    private final Boolean accept;
    private final Boolean reject;
    private final Boolean cancel;

    public Request(final String id, final Date schedule, final String state, final Map<String, ?> info) {
        this.id = id;
        this.schedule = schedule;
        this.state = state;

        this.accept = getTriState(info, "acceptRequest");
        this.reject = getTriState(info, "rejectRequest");
        this.cancel = getTriState(info, "cancelRequest");
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

    public Map<String, ?> getInfo() {
        Map<String, Boolean> info = new MiniMap<>(3);
        info.put("acceptRequest", accept);
        info.put("rejectRequest", reject);
        info.put("cancelRequest", cancel);
        return info;
    }

}

