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
package org.hippoecm.repository.api;

/**
 * WARNING: this api is not yet stable!
 * The possible actions specified by the <code>mergeBehavior</code>
 * parameter in {@link HippoSession#importDereferencedXML}, and
 * {@link HippoSession#getDereferencedImportContentHandler}.
 */
public interface ImportMergeBehavior {
    final static String SVN_ID = "$Id$";

    public static final int IMPORT_MERGE_SKIP = 0;
    public static final int IMPORT_MERGE_OVERWRITE = 1;
    public static final int IMPORT_MERGE_ADD_OR_SKIP = 2; // try add first else skip
    public static final int IMPORT_MERGE_ADD_OR_OVERWRITE = 3; //try add first else overwrite
    public static final int IMPORT_MERGE_THROW = 4;
}