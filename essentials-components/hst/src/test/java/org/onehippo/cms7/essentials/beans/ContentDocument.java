/*
 * Copyright 2016-2023 Bloomreach
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

package org.onehippo.cms7.essentials.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

import java.util.Calendar;

@Node(jcrType = "myhippoproject:contentdocument")
public class ContentDocument extends BaseDocument {
        public String getIntroduction() {
        return getSingleProperty("myhippoproject:introduction");
    }

        public String getTitle() {
        return getSingleProperty("myhippoproject:title");
    }

        public HippoHtml getContent() {
        return getHippoHtml("myhippoproject:content");
    }

        public Calendar getPublicationDate() {
        return getSingleProperty("myhippoproject:publicationdate");
    }
}
