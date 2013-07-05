/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.content;

import javax.jcr.Value;

import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

/**
 */
public interface ContentNode extends ContentItem {

    ContentType getContentType();
    ContentTypeChild getContentTypeChild();

    boolean isChild();

    String getIdentifier();
    int getIndex();
    //? boolean isCheckedOut();
    boolean canAddMixin(String mixinName);
    void addMixin(String mixinName);
    void removeMixin(String mixinName);
    String[] getNodeMixins();
    boolean isContentType(String contentTypeName);
    ContentItem getPrimaryItem();
    ContentType getPrimaryContentType();
    //? void setPrimaryContentType(String contentTypeName);
    boolean hasChildren();
    boolean hasChild(String name);
    ContentNode getChild(String name);
    RangeIterable<ContentNode> getChildren();
    RangeIterable<ContentNode> getChildren(String namePattern);
    RangeIterable<ContentNode> getChildren(String[] nameGlob);
    //? ContentNodeIterator getChildrenByPrimaryType(String ContentTypeName);
    ContentNode addChild(String name);
    ContentNode addChild(String name, String primaryNodeTypeName);
    ContentNode removeChild(String name);
    void orderBefore(String scrChildName, String targetChildName);
    boolean hasProperty(String name);
    boolean hasProperties();
    ContentProperty getProperty(String name);
    RangeIterable<ContentProperty> getProperties();
    RangeIterable<ContentProperty> getProperties(String namePattern);
    RangeIterable<ContentProperty> getProperties(String[] nameGlob);
    ContentProperty setProperty(String name, Value value);
    ContentProperty setProperty(String name, Value[] values);
    ContentProperty removeProperty(String name);

    boolean isDetached();
    void detach();
}
