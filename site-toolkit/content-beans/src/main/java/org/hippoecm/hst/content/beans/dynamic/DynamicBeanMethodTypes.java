/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

public enum DynamicBeanMethodTypes {

    STRING() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(String[].class, "getStringValues");
            } else {
                return new ImmutablePair<Class<?>, String>(String.class, "getStringValue");
            }
        }
    },

    BOOLEAN() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(Boolean[].class, "getBooleanValues");
            } else {
                return new ImmutablePair<Class<?>, String>(Boolean.class, "getBooleanValue");
            }
        }
    },

    DOUBLE() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(Double[].class, "getDoubleValues");
            } else {
                return new ImmutablePair<Class<?>, String>(Double.class, "getDoubleValue");
            }
        }
    },

    LONG() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(Long[].class, "getLongValues");
            } else {
                return new ImmutablePair<Class<?>, String>(Long.class, "getLongValue");
            }
        }
    },

    DOCBASE() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(List.class, "getDocbaseValues");
            } else {
                return new ImmutablePair<Class<?>, String>(HippoBean.class, "getDocbaseValue");
            }
        }
    },

    DATE() {
        @Override
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {

            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(Calendar[].class, "getDateValues");
            } else {
                return new ImmutablePair<Class<?>, String>(Calendar.class, "getDateValue");
            }
        }
    },

    IMAGE() {
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(List.class, "getImageValues");
            } else {
                return new ImmutablePair<Class<?>, String>(HippoGalleryImageSet.class, "getImageValue");
            }
        }

    },

    HTML() {
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(List.class, "getHtmlValues");
            } else {
                return new ImmutablePair<Class<?>, String>(HippoHtml.class, "getHtmlValue");
            }
        }

    },

    DOCUMENT() {
        public Pair<Class<?>, String> getMethod(boolean hasMultiple) {
            if (hasMultiple) {
                return new ImmutablePair<Class<?>, String>(List.class, "getLinkedDocuments");
            } else {
                return new ImmutablePair<Class<?>, String>(HippoBean.class, "getLinkedDocument");
            }
        }
    };

    public abstract Pair<Class<?>, String> getMethod(boolean hasMultiple);

}
