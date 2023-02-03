/*
 *  Copyright 2010-2023 Bloomreach
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
package org.hippoecm.frontend.translation.components.document;

import java.util.LinkedList;
import java.util.List;

public class DocumentRegularPage extends DocumentTranslationPage {

    @Override
    protected final List<FolderTranslation> getFolderTranslations() {
        List<FolderTranslation> fts = new LinkedList<FolderTranslation>();
        
        FolderTranslation top = new FolderTranslation("top");
        top.setType("folder");
        top.setName("Top");
        top.setUrl("top");
        top.setNamefr("Hoogste");
        top.setUrlfr("hoogste");
        top.setEditable(false);
        fts.add(top);

        FolderTranslation sub = new FolderTranslation("sub");
        sub.setType("folder");
        sub.setName("Sub");
        sub.setUrl("sub");
        sub.setNamefr("Onder");
        sub.setUrlfr("onder");
        sub.setEditable(true);
        fts.add(sub);

        FolderTranslation doc = new FolderTranslation("doc");
        doc.setType("doc");
        doc.setName("Document");
        doc.setUrl("document");
        doc.setEditable(true);
        fts.add(doc);

        return fts;
    }
}
