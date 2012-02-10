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
package org.hippoecm.audit;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Session;
import org.hippoecm.frontend.session.UserSession;

import net.sf.json.JSONObject;

/**
 * Audit event is wrapper class over JSON object to hide json specific implementation
 * It contains some of default categories of auditing
 */
public class HippoEvent {

    public static final String CATEGORY_USER_MANAGEMENT = "user-management";
    public static final String CATEGORY_GROUP_MANAGEMENT = "group-management";
    public static final String CATEGORY_PERMISSIONS_MANAGEMENT = "permissions-management";
    public static final String CATEGORY_SECURITY = "security";

    public static final String APPLICATION = "application";
    public static final String RESULT = "result";
    public static final String USER = "user";
    public static final String ACTION = "action";
    public static final String CATEGORY = "category";
    public static final String MESSAGE = "message";


    private Map<String,Object> values = new HashMap<String, Object>();
    


    public HippoEvent() {
        values.put(APPLICATION,"cms");
        values.put(RESULT,"success");
    }

    public HippoEvent user(String user) {
        values.put(USER, user);
        return this;
    }

    public HippoEvent user(Session session) {
        values.put(USER, ((UserSession)session.get()).getJcrSession().getUserID());
        return this;
    }

    public HippoEvent action(String action) {
        values.put(ACTION, action);
        return this;
    }

    public HippoEvent category(String category) {
        values.put(CATEGORY, category);
        return this;
    }

    public HippoEvent application(String application) {
        values.put(APPLICATION, application);
        return this;
    }

    public HippoEvent result(String result) {
        values.put(RESULT, result);
        return this;
    }

    public HippoEvent message(String message) {
        values.put(MESSAGE, message);
        return this;
    }

    public HippoEvent put(String key,Object value) {
        values.put(key, value);
        return this;
    }


    @Override
    public String toString()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.putAll(values);
        return jsonObject.toString();

    }
}
