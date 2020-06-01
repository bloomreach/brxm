/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.translation;

/**
 * Node types used by content localization.
 */
public interface HippoTranslationNodeType {

    String NS = "http://www.onehippo.org/jcr/hippotranslation/nt/1.0";
    
    String NT_TRANSLATED = "hippotranslation:translated";

    /** use {@link HippoTranslatedNode} to find the same node in other languages */
    @Deprecated
    String NT_TRANSLATIONS = "hippotranslation:translations";

    String ID = "hippotranslation:id";
    String LOCALE = "hippotranslation:locale";
    String SOURCELOCALE = "hippotranslation:sourcelocale";

    /** use {@link HippoTranslatedNode} to find the same node in other languages */
    @Deprecated
    String TRANSLATIONS = "hippotranslation:translations";

}
