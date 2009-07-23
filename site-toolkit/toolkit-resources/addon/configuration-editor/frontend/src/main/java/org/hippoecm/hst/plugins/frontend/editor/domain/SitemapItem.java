/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.hippoecm.frontend.model.JcrNodeModel;

public class SitemapItem extends EditorBean {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public enum Matcher {
        CUSTOM(""), WILDCARD("*"), INFINITE("**");

        private String value;

        Matcher(String initialValue) {
            value = initialValue;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    };

    public SitemapItem(JcrNodeModel model) {
        super(model);
    }

    String matcher;
    String contentPath;
    String page;
    String portlet;

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPortlet() {
        return portlet;
    }

    public void setPortlet(String portlet) {
        this.portlet = portlet;
    }

}
