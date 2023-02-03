/*
 *  Copyright 2012-2023 Bloomreach
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
package org.onehippo.cms7.event;

public class HippoSecurityEvent<T extends HippoSecurityEvent<T>> extends HippoEvent<T> {

    private static final String SUCCESS = "success";

    public HippoSecurityEvent(String application) {
        super(application);
        category(HippoEventConstants.CATEGORY_SECURITY);
    }

    public HippoSecurityEvent(HippoEvent<?> event) {
        super(event);
    }

    public T success(Boolean success) {
        return put(SUCCESS, success);
    }

    public Boolean success() {
        return get(SUCCESS);
    }

}
