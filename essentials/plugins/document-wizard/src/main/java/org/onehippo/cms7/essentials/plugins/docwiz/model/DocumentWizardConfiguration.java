/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.docwiz.model;

public class DocumentWizardConfiguration {
    private String shortcutName;       // internal (node) name of the dashboard shortcut
    private String classificationType; // 'list' or 'date'
    private String documentType;       // JCR type name
    private String baseFolder;         // for creating new documents
    private String valueListPath;      // JCR path to valueList document, if classification is 'list'
    private String documentQuery;      // 'query' to use for creating a new document

    private String shortcutLinkLabel;
    private String nameLabel;
    private String listLabel;
    private String dateLabel;

    public String getShortcutName() {
        return shortcutName;
    }

    public void setShortcutName(final String shortcutName) {
        this.shortcutName = shortcutName;
    }

    public String getClassificationType() {
        return classificationType;
    }

    public void setClassificationType(final String classificationType) {
        this.classificationType = classificationType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public String getBaseFolder() {
        return baseFolder;
    }

    public void setBaseFolder(final String baseFolder) {
        this.baseFolder = baseFolder;
    }

    public String getValueListPath() {
        return valueListPath;
    }

    public void setValueListPath(final String valueListPath) {
        this.valueListPath = valueListPath;
    }

    public String getDocumentQuery() {
        return documentQuery;
    }

    public void setDocumentQuery(final String documentQuery) {
        this.documentQuery = documentQuery;
    }

    public String getShortcutLinkLabel() {
        return shortcutLinkLabel;
    }

    public void setShortcutLinkLabel(final String shortcutLinkLabel) {
        this.shortcutLinkLabel = shortcutLinkLabel;
    }

    public String getNameLabel() {
        return nameLabel;
    }

    public void setNameLabel(final String nameLabel) {
        this.nameLabel = nameLabel;
    }

    public String getListLabel() {
        return listLabel;
    }

    public void setListLabel(final String listLabel) {
        this.listLabel = listLabel;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(final String dateLabel) {
        this.dateLabel = dateLabel;
    }
}
