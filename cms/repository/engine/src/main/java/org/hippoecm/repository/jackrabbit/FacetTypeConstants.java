/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

/**
 * This class contains Character constants to use to prepend a facet lucene field to
 * be able to distinguish between Date, String, Long, Double and Boolean   
 */
public interface FacetTypeConstants {
    public final static Character STRING_POSTFIX = '\uFFFF';
    public final static Character BOOLEAN_POSTFIX = '\uFFFE';
    public final static Character LONG_POSTFIX = '\uFFFD';
    public final static Character DOUBLE_POSTFIX = '\uFFFC';
    public final static Character DATE_POSTFIX = '\uFFFB';
    public final static Character[] POSTFIXES = {STRING_POSTFIX,BOOLEAN_POSTFIX,LONG_POSTFIX,DOUBLE_POSTFIX,DATE_POSTFIX};

    public final static int STRING = 0;
    public final static int BOOLEAN = 1;
    public final static int LONG = 2;
    public final static int DOUBLE = 3;
    public final static int DATE = 4;
}
