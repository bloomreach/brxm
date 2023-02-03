/*
 * Copyright 2013-2023 Bloomreach
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
package org.onehippo.cms7.services.search.commons;

import org.onehippo.cms7.services.search.annotation.Content;
import org.onehippo.cms7.services.search.annotation.Field;

@Content(primaryTypeName="foo")
public interface Foo {

    @Field(name="id")
    public String getId();

    public void setId(String id);

    @Field(name="message")
    public String getMessage();

    public void setMessage(String message);

    @Field(name="active")
    public Boolean isActive();

    public void setActive(Boolean active);

    public void foo();

}
