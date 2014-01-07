/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.reports.plugins.brokenlinkslist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.reviewedactions.HippoStdPubWfNodeType;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.brokenlinks.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtDataField;

public class BrokenLinksListColumns implements IClusterable {

    private static final long serialVersionUID = 1L;
    public static final String COLUMN_PROP_HEADER = "header";
    public static final String COLUMN_PROP_WIDTH = "width";

    public static enum ColumnName { createdBy, creationDate, lastModificationDate, lastModifiedBy, name, path, publicationDate, brokenlinksLinks, brokenlinksBrokenSince, brokenlinksStatus};

    // Supported status codes, meaning there are translations and maybe custom messages for these codes
    private static final List<Integer> SUPPORTED_HTTP_STATUS_CODES = Arrays.asList(301, 400, 401, 403, 404, 405, 414, 415, 500, 502, 503);
    private static final Map<ColumnName, IBrokenLinkDocumentListColumn> DOCUMENT_COLUMN_MAP = new EnumMap<ColumnName, IBrokenLinkDocumentListColumn>(ColumnName.class);

    static {
        DOCUMENT_COLUMN_MAP.put(ColumnName.createdBy, new StringPropertyColumn("createdBy", HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATED_BY));
        DOCUMENT_COLUMN_MAP.put(ColumnName.creationDate, new DatePropertyColumn("creationDate", HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE));
        DOCUMENT_COLUMN_MAP.put(ColumnName.lastModificationDate, new DatePropertyColumn("lastModificationDate", HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE));
        DOCUMENT_COLUMN_MAP.put(ColumnName.lastModifiedBy, new StringPropertyColumn("lastModifiedBy", HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY));
        DOCUMENT_COLUMN_MAP.put(ColumnName.name, new NameColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.path, new PathColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.publicationDate, new DatePropertyColumn("publicationDate", HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE));

        // Broken Links specific columns. Unfortunately too much must be done for every column, so we cannot have something more generic like BrokenLinksPropertyColumn
        DOCUMENT_COLUMN_MAP.put(ColumnName.brokenlinksLinks, new BrokenLinksLinksColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.brokenlinksBrokenSince, new BrokenLinksBrokenSinceColumn());
        DOCUMENT_COLUMN_MAP.put(ColumnName.brokenlinksStatus, new BrokenLinksStatusColumn());
    }

    private static final Logger log = LoggerFactory.getLogger(BrokenLinksListColumns.class);

    private final List<ColumnName> columnNames;

    public BrokenLinksListColumns(String[] names) {
        columnNames = new ArrayList<ColumnName>(names.length + 1);

        if (names.length == 0) {
            log.warn("No column names specified, expected a comma-separated list with these possible values: {}",
                    allColumnNames());
        }

        for (String name: names) {
            try {
                final ColumnName columnName = ColumnName.valueOf(name);
                if (!columnName.equals(ColumnName.path)) {
                    columnNames.add(columnName);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring unknown document list column name: '{}', known names are: {}", name, allColumnNames());
            }
        }

        // always add the path column since it is used internally to identify the document
        columnNames.add(ColumnName.path);
    }

    public List<IBrokenLinkDocumentListColumn> getAllColumns() {
        List<IBrokenLinkDocumentListColumn> result = new ArrayList<IBrokenLinkDocumentListColumn>(columnNames.size());

        for (ColumnName columnName: columnNames) {
            final IBrokenLinkDocumentListColumn column = DOCUMENT_COLUMN_MAP.get(columnName);
            result.add(column);
        }

        return result;
    }

    public boolean containsColumn(String name) {
        if (name == null) {
            return false;
        }
        try {
            return columnNames.contains(ColumnName.valueOf(name));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<ExtDataField> getAllExtFields() {
        List<ExtDataField> result = new ArrayList<ExtDataField>(columnNames.size());

        for (IBrokenLinkDocumentListColumn column: getAllColumns()) {
            result.add(column.getExtField());
        }

        return result;
    }

    public JSONArray getAllColumnConfigs() throws JSONException {
        JSONArray result = new JSONArray();

        for (IBrokenLinkDocumentListColumn column: getAllColumns()) {
            result.put(column.getExtColumnConfig());
        }

        return result;
    }

    private static String allColumnNames() {
        StringBuilder result = new StringBuilder();
        String concat = "";
        for (ColumnName columnName: ColumnName.values()) {
            if (columnName != ColumnName.path) {
                result.append(concat);
                result.append('\'');
                result.append(columnName.name());
                result.append('\'');
                concat = ",";
            }
        }
        return result.toString();
    }

    private static String getResourceValue(String key) {
        return new ClassResourceModel(key, BrokenLinksListColumns.class).getObject();
    }

    // ==================================== Columns ====================================

    private static class StringPropertyColumn implements IBrokenLinkDocumentListColumn {

        private final String name;
        private final String property;

        public StringPropertyColumn(String name, String property) {
            this.name = name;
            this.property = property;
        }

        public String getProperty() {
            return property;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", name);
            config.put("id", name);
            config.put(COLUMN_PROP_HEADER, getResourceValue("column-" + name + "-header"));
            config.put(COLUMN_PROP_WIDTH, Integer.parseInt(getResourceValue("column-" + name + "-width")));
            return config;
        }

        @Override
        public ExtDataField getExtField() {
            return new ExtDataField(name);
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property prop = node.getProperty(property);

            if (prop == null) {
                return StringUtils.EMPTY;
            }

            return prop.getString();
        }

    }

    private static class DatePropertyColumn extends StringPropertyColumn {

        public DatePropertyColumn(String name, String property) {
            super(name, property);
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            Property dateProperty = node.getProperty(getProperty());

            if (dateProperty == null) {
                return StringUtils.EMPTY;
            }

            Locale locale = Session.get().getLocale();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withLocale(locale);

            try {
                final DateTime date = new DateTime(dateProperty.getDate());
                return formatter.print(date);
            } catch (IllegalArgumentException e) {
                log.warn("Could not parse property '{}': " + e.getMessage() + ", using empty string instead", getProperty(), e.getMessage());
            }

            return StringUtils.EMPTY;
        }
    }

    private static class NameColumn implements IBrokenLinkDocumentListColumn {

        private static final String DATA_INDEX = "name";
        private static final ExtDataField EXT_FIELD = new ExtDataField(DATA_INDEX);

        @Override
        public ExtDataField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("dataIndex", DATA_INDEX);
            config.put("id", DATA_INDEX);
            config.put(COLUMN_PROP_HEADER, getResourceValue("column-name-header"));
            config.put(COLUMN_PROP_WIDTH, Integer.parseInt(getResourceValue("column-name-width")));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            if (node instanceof HippoNode) {
                HippoNode hippoNode = (HippoNode) node;
                return hippoNode.getLocalizedName();
            } else {
                return NodeNameCodec.decode(node.getName());
            }
        }
    }

    private static class PathColumn implements IBrokenLinkDocumentListColumn {

        private static final String DATA_INDEX = "path";
        private static final ExtDataField EXT_FIELD = new ExtDataField(DATA_INDEX);

        @Override
        public ExtDataField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            // never include the path as a visible column
            return null;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            return node.getPath();
        }
    }

    private static class BrokenLinksLinksColumn implements IBrokenLinkDocumentListColumn {

        private static final ExtDataField EXT_FIELD = new ExtDataField("brokenlinksLinks");

        @Override
        public ExtDataField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("id", "brokenlinksLinks");
            config.put(COLUMN_PROP_HEADER, getResourceValue("column-brokenlinksLinks-header"));
            config.put(COLUMN_PROP_WIDTH, Integer.parseInt(getResourceValue("column-brokenlinksLinks-width")));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            StringBuilder aggregateCell = new StringBuilder();
            NodeIterator linksIterator = node.getNodes("brokenlinks:link");
            while(linksIterator.hasNext()){
                final Node linkNode = (Node) linksIterator.next();
                if (linkNode.hasProperty("brokenlinks:excerpt")){
                    aggregateCell.append(linkNode.getProperty("brokenlinks:excerpt").getString());
                } else {
                    aggregateCell.append(linkNode.getProperty("brokenlinks:url").getString());
                }
                aggregateCell.append("<br/>");
            }
            return aggregateCell.toString();
        }
    }

    private static class BrokenLinksBrokenSinceColumn implements IBrokenLinkDocumentListColumn {

        private static final ExtDataField EXT_FIELD = new ExtDataField("brokenlinksBrokenSince");

        @Override
        public ExtDataField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("id", "brokenlinksBrokenSince");
            config.put(COLUMN_PROP_HEADER, getResourceValue("column-brokenlinksBrokenSince-header"));
            config.put(COLUMN_PROP_WIDTH, Integer.parseInt(getResourceValue("column-brokenlinksBrokenSince-width")));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            StringBuilder aggregateCell = new StringBuilder();
            Locale locale = Session.get().getLocale();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").withLocale(locale);

            NodeIterator linksIterator = node.getNodes("brokenlinks:link");
            Property dateProperty = null;
            while(linksIterator.hasNext()){
                final Node linkNode = (Node) linksIterator.next();
                try {
                    if (linkNode.hasProperty("brokenlinks:brokenSince")){
                        dateProperty = linkNode.getProperty("brokenlinks:brokenSince");
                        aggregateCell.append(formatter.print(new DateTime(dateProperty.getDate())));
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Could not parse property '{}': " + e.getMessage() + ", using empty string instead", dateProperty, e.getMessage());
                }
                aggregateCell.append("<br/>");
            }
            return aggregateCell.toString();
        }
    }

    private static class BrokenLinksStatusColumn implements IBrokenLinkDocumentListColumn {

        private static final ExtDataField EXT_FIELD = new ExtDataField("brokenlinksStatus");

        @Override
        public ExtDataField getExtField() {
            return EXT_FIELD;
        }

        @Override
        public JSONObject getExtColumnConfig() throws JSONException {
            JSONObject config = new JSONObject();
            config.put("id", "brokenlinksStatus");
            config.put(COLUMN_PROP_HEADER, getResourceValue("column-brokenlinksStatus-header"));
            config.put(COLUMN_PROP_WIDTH, Integer.parseInt(getResourceValue("column-brokenlinksStatus-width")));
            return config;
        }

        @Override
        public String getValue(final Node node) throws RepositoryException {
            StringBuilder aggregateCell = new StringBuilder();
            NodeIterator linksIterator = node.getNodes("brokenlinks:link");

            while(linksIterator.hasNext()){
                final Node linkNode = (Node) linksIterator.next();
                String statusMessage = "";
                String errorMessage = linkNode.hasProperty("brokenlinks:errorMessage") ? linkNode.getProperty("brokenlinks:errorMessage").getString() : "";
                int statusCode = linkNode.hasProperty("brokenlinks:errorCode") ?
                        (int) linkNode.getProperty("brokenlinks:errorCode").getLong() :
                        Link.ERROR_CODE;

                if (statusCode == Link.ERROR_CODE) {
                    statusMessage = errorMessage;
                } else if (SUPPORTED_HTTP_STATUS_CODES.contains(statusCode)) {
                    statusMessage = getResourceValue("httpstatus-" + statusCode);
                } else if (statusCode == Link.EXCEPTION_CODE) {
                    try {
                        statusMessage = getResourceValue("exception-" + errorMessage);
                    } catch (Exception e) {
                        statusMessage = getResourceValue("exception-generic").concat(": " + errorMessage);
                    }
                } else {
                    statusMessage = getResourceValue("httpstatus")+" ("+statusCode+")";
                }

                aggregateCell.append(statusMessage).append("<br/>");
            }
            return aggregateCell.toString();
        }
    }



}
