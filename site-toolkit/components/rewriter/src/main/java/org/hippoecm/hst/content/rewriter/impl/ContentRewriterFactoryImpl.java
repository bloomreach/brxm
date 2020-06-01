/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.rewriter.impl;

import org.hippoecm.hst.content.rewriter.ContentRewriter;
import org.hippoecm.hst.content.rewriter.ContentRewriterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentRewriterFactoryImpl implements ContentRewriterFactory {

    private static Logger log = LoggerFactory.getLogger(ContentRewriterFactoryImpl.class);
    private String defaultContentRewriterClassName;

    @SuppressWarnings("unused")
    public String getDefaultContentRewriterClassName() {
        return defaultContentRewriterClassName;
    }
    public void setDefaultContentRewriterClassName(String defaultContentRewriterClassName) {
        this.defaultContentRewriterClassName = defaultContentRewriterClassName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ContentRewriter<String> createContentRewriter() {
        try {
            Class<ContentRewriter<String>> clazz = (Class<ContentRewriter<String>>) Class.forName(defaultContentRewriterClassName);
            return clazz.newInstance();
        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
            String error = "Cannot load the ContentRewriter class " + defaultContentRewriterClassName;
            log.error(error, e);
            throw new RuntimeException(error, e);
        }
    }
}
