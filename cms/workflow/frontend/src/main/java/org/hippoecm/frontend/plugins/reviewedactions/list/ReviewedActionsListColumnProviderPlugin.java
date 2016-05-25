/*
 *  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.list;

import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.list.comparators.DocumentAttributeComparator;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.DocumentAttributeAttributeModifier;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.DocumentAttributeRenderer;
import org.hippoecm.frontend.plugins.reviewedactions.list.resolvers.StateIconAttributes;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.datetime.DateTimePrinter;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.hippoecm.frontend.skin.DocumentListColumn;

public class ReviewedActionsListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    public ReviewedActionsListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        return Collections.emptyList();
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = new ArrayList<>();
        columns.add(createLastModifiedDateColumn());
        columns.add(createLastModifiedByColumn());
        columns.add(createPublicationDateColumn());
        return columns;
    }

    private ListColumn<Node> createLastModifiedDateColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-lastmodified-date", getClass());
        final ListColumn<Node> column = new ListColumn<>(displayModel, "lastmodified-date");
        column.setComparator(new DocumentAttributeComparator() {
            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getLastModifiedDate(), s2.getLastModifiedDate());
            }
        });
        column.setCssClass(DocumentListColumn.DATE.getCssClass());
        column.setRenderer(new DocumentAttributeRenderer() {
            @Override
            protected String getObject(StateIconAttributes atts) {
                Calendar lastModified = atts.getLastModifiedDate();
                return DateTimePrinter.of(lastModified).print();
            }
        });
        column.setAttributeModifier(new DocumentAttributeAttributeModifier("title") {
            @Override
            protected String getObject(StateIconAttributes atts) {
                Calendar lastModified = atts.getLastModifiedDate();
                return DateTimePrinter.of(lastModified).print(FormatStyle.FULL);
            }
        });
        return column;
    }

    private ListColumn<Node> createLastModifiedByColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-lastmodified-by", getClass());
        final ListColumn<Node> column = new ListColumn<>(displayModel, "lastmodified-by");
        column.setComparator(new DocumentAttributeComparator() {
            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                if (s1.getLastModifiedBy() == null || s2.getLastModifiedBy() == null)
                    return 0;
                return s1.getLastModifiedBy().compareTo(s2.getLastModifiedBy());
            }
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            @Override
            protected String getObject(StateIconAttributes atts) {
                return atts.getLastModifiedBy();
            }
        });
        column.setCssClass(DocumentListColumn.LAST_MODIFIED_BY.getCssClass());
        return column;
    }

    private ListColumn<Node> createPublicationDateColumn() {
        final ClassResourceModel displayModel = new ClassResourceModel("doclisting-publication-date", getClass());
        final ListColumn<Node> column = new ListColumn<>(displayModel, "publication-date");
        column.setComparator(new DocumentAttributeComparator() {
            @Override
            protected int compare(StateIconAttributes s1, StateIconAttributes s2) {
                return compareDates(s1.getPublicationDate(), s2.getPublicationDate());
            }
        });
        column.setRenderer(new DocumentAttributeRenderer() {
            @Override
            protected String getObject(StateIconAttributes atts) {
                final Calendar publicationDate = atts.getPublicationDate();
                return DateTimePrinter.of(publicationDate).print();
            }
        });
        column.setAttributeModifier(new DocumentAttributeAttributeModifier("title") {
            @Override
            protected String getObject(StateIconAttributes atts) {
                final Calendar publicationDate = atts.getPublicationDate();
                return DateTimePrinter.of(publicationDate).print(FormatStyle.FULL);
            }
        });
        column.setCssClass(DocumentListColumn.DATE.getCssClass());
        return column;
    }

    private int compareDates(Calendar o1, Calendar o2) {
        if (o1 == null) {
            if (o2 == null) {
                return 0;
            }
            return 1;
        } else if (o2 == null) {
            return -1;
        }
        return o1.compareTo(o2);
    }
}
