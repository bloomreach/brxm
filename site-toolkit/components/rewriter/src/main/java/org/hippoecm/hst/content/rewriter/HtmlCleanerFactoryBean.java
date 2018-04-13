/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.rewriter;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class HtmlCleanerFactoryBean extends AbstractFactoryBean<HtmlCleaner> {

    private boolean translateSpecialEntities = false;
    private boolean omitXmlDeclaration = true;
    private boolean recognizeUnicodeChars = false;
    private boolean omitComments = true;
    private boolean addNewlineToHeadAndBody = false;
    private boolean advancedXmlEscape = true;
    private boolean transResCharsToNCR = true;
    private boolean useEmptyElementTags = false;

    public void setTranslateSpecialEntities(final boolean translateSpecialEntities) {
        this.translateSpecialEntities = translateSpecialEntities;
    }

    public void setOmitXmlDeclaration(final boolean omitXmlDeclaration) {
        this.omitXmlDeclaration = omitXmlDeclaration;
    }

    public void setRecognizeUnicodeChars(final boolean recognizeUnicodeChars) {
        this.recognizeUnicodeChars = recognizeUnicodeChars;
    }

    public void setOmitComments(final boolean omitComments) {
        this.omitComments = omitComments;
    }

    public void setAddNewlineToHeadAndBody(final boolean addNewlineToHeadAndBody) {
        this.addNewlineToHeadAndBody = addNewlineToHeadAndBody;
    }

    public void setAdvancedXmlEscape(final boolean advancedXmlEscape) {
        this.advancedXmlEscape = advancedXmlEscape;
    }

    public void setTransResCharsToNCR(final boolean transResCharsToNCR) {
        this.transResCharsToNCR = transResCharsToNCR;
    }

    public void setUseEmptyElementTags(final boolean useEmptyElementTags) {
        this.useEmptyElementTags = useEmptyElementTags;
    }

    @Override
    public Class<?> getObjectType() {
        return HtmlCleaner.class;
    }

    @Override
    protected HtmlCleaner createInstance() throws Exception {

        final HtmlCleaner cleaner = new HtmlCleaner();
        final CleanerProperties properties = cleaner.getProperties();
        // the ContentRestApiHtmlRewriter.ContentParser expects html/body element to take the content from in the end
        properties.setOmitHtmlEnvelope(false);

        properties.setTranslateSpecialEntities(translateSpecialEntities);
        properties.setOmitXmlDeclaration(omitXmlDeclaration);
        properties.setRecognizeUnicodeChars(recognizeUnicodeChars);
        properties.setOmitComments(omitComments);
        properties.setAddNewlineToHeadAndBody(addNewlineToHeadAndBody);
        properties.setAdvancedXmlEscape(advancedXmlEscape);
        properties.setTransResCharsToNCR(transResCharsToNCR);
        properties.setUseEmptyElementTags(useEmptyElementTags);
        return cleaner;
    }
}
