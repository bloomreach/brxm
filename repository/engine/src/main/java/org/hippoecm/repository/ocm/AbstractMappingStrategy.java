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
package org.hippoecm.repository.ocm;

import java.util.Calendar;
import java.util.Date;
import javax.jcr.Node;
import javax.jcr.Session;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.store.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMappingStrategy {

    protected final Logger log = LoggerFactory.getLogger(AbstractMappingStrategy.class);

    /** The state manager. */
    protected ObjectProvider op;

    /** The field meta data for the given field. */
    protected AbstractMemberMetaData mmd;

    /** The type of the field meta data. */
    protected Class type;

    /** The JCR name of the field meta data. */
    protected String name;
    
    /** The JCR Session */
    protected Session session;

    /** The node to perform the update on */
    protected Node node;
    
    protected ColumnResolver columnResolver;
    protected TypeResolver typeResolver;

    /**
     * Instantiates a new abstract mapping strategy.
     * @param op the state manager
     * @param fieldNumber the field number
     * @param attributes the attributes
     */
    protected AbstractMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session, ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        this.op = op;
        this.mmd = mmd;
        this.session = session;
        this.columnResolver = columnResolver;
        this.typeResolver = typeResolver;
        this.node = node;
        this.type = mmd.getType();
        this.name = mmd.getName();
    }

    /**
     * Inserts the given value(s) into the JCR repository.
     * @param value the value(s)
     */
    public abstract void insert(Object value);

    /**
     * Updates the given value(s) in the JCR repository.
     * @param value the value(s
     */
    public abstract void update(Object value);

    /**
     * Fetches the value(s) from the JCR repository
     * @return the value(s)
     */
    public abstract Object fetch();


    public static AbstractMappingStrategy findMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session,
            ColumnResolver columnResolver, TypeResolver typeResolver) {
        return findMappingStrategy(op, mmd, session, columnResolver, typeResolver, null);
}
    /**
     * Finds the mapping strategy for the specified field of the state manager.
     * @param op state manager
     * @param fieldNumber the field number
     * @param session for persisting and retrieving the properties and nodes
     * @return the mapping strategy, null if now appropriate mapping strategy exists
     */
    public static AbstractMappingStrategy findMappingStrategy(ObjectProvider op, AbstractMemberMetaData mmd, Session session,
            ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        MetaDataManager mmgr = op.getExecutionContext().getMetaDataManager();

        Class type = mmd.getType();
        boolean isArray = type.isArray();
        boolean isCollection = mmd.hasCollection();
        if (isArray) {
            type = type.getComponentType();
        } else if (isCollection) {
            type = op.getExecutionContext().getClassLoaderResolver().classForName(mmd.getCollection().getElementType());
        }

        if (type.isPrimitive() || String.class.isAssignableFrom(type) || Number.class.isAssignableFrom(type)
                || Character.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type) || Calendar.class.isAssignableFrom(type) || type.isEnum()) {
            if (isArray) {
                return new ArrayMappingStrategy(op, mmd, session, columnResolver, typeResolver, node);
            } else if (isCollection) {
                // FIXME: implement
                //return new CollectionMappingStrategy(sm, mmd, session);
            } else {
                return new ValueMappingStrategy(op, mmd, session, columnResolver, typeResolver, node);
            }
        } else {
            return new ValueMappingStrategy(op, mmd, session, columnResolver, typeResolver, node);
        }
        return null;
    }
}
