/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.decorating.DecoratorFactory;

class RowIteratorDecorator implements RowIterator {

    private final DecoratorFactory factory;
    private final Session session;
    private final RowIterator rows;

    public RowIteratorDecorator(final DecoratorFactory factory, final Session session, final RowIterator rows) {
        this.factory = factory;
        this.session = session;
        this.rows = rows;
    }

    @Override
    public Row nextRow() {
        return next();
    }

    @Override
    public void skip(final long skipNum) {
        rows.skip(skipNum);
    }

    @Override
    public long getSize() {
        return rows.getSize();
    }

    @Override
    public long getPosition() {
        return rows.getPosition();
    }

    @Override
    public boolean hasNext() {
        return rows.hasNext();
    }

    @Override
    public Row next() {
        final Row row = rows.nextRow();
        return new Row() {
            @Override
            public Value[] getValues() throws RepositoryException {
                return row.getValues();
            }

            @Override
            public Value getValue(final String columnName) throws RepositoryException {
                return row.getValue(columnName);
            }

            @Override
            public Node getNode() throws RepositoryException {
                return decorateNode(row.getNode());
            }

            private Node decorateNode(final Node node) {
                if (node instanceof Version) {
                    return factory.getVersionDecorator(session, (Version) node);
                } else if (node instanceof VersionHistory) {
                    return factory.getVersionHistoryDecorator(session, (VersionHistory) node);
                } else {
                    return factory.getNodeDecorator(session, node);
                }
            }

            @Override
            public Node getNode(final String selectorName) throws RepositoryException {
                return decorateNode(row.getNode(selectorName));
            }

            @Override
            public String getPath() throws RepositoryException {
                return row.getPath();
            }

            @Override
            public String getPath(final String selectorName) throws RepositoryException {
                return row.getPath(selectorName);
            }

            @Override
            public double getScore() throws RepositoryException {
                return row.getScore();
            }

            @Override
            public double getScore(final String selectorName) throws RepositoryException {
                return row.getScore(selectorName);
            }
        };
    }

    @Override
    public void remove() {
        rows.remove();
    }
}
