/*
 *  Copyright 2009 Hippo.
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

import javax.jcr.Node;
import javax.jcr.Session;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateFieldManager extends AbstractFieldManager {

    protected final Logger log = LoggerFactory.getLogger(UpdateFieldManager.class);

    private ObjectProvider op;
    private Session session;
    private Node node;
    private ColumnResolver columnResolver;
    private TypeResolver typeResolver;

    public UpdateFieldManager(ObjectProvider op, Session session, ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        this.op = op;
        this.session = session;
        this.node = node;
        this.columnResolver = columnResolver;
        this.typeResolver = typeResolver;
    }

    @Override
    public void storeObjectField(int fieldNumber, Object value) {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        AbstractMappingStrategy ms = AbstractMappingStrategy.findMappingStrategy(op, mmd, session, columnResolver, typeResolver, node);
        if (ms != null) {
            ms.update(value);
            return;
        }
        throw new NucleusException("Field " + mmd.getFullFieldName() + " cannot be persisted because type="
                + mmd.getTypeName() + " is not supported for this datastore");
    }

    @Override
    public void storeBooleanField(int fieldNumber, boolean value) {
        storeObjectField(fieldNumber, Boolean.valueOf(value));
    }

    @Override
    public void storeByteField(int fieldNumber, byte value) {
        storeObjectField(fieldNumber, Byte.valueOf(value));
    }

    @Override
    public void storeCharField(int fieldNumber, char value) {
        storeObjectField(fieldNumber, String.valueOf(value));
    }

    @Override
    public void storeDoubleField(int fieldNumber, double value) {
        storeObjectField(fieldNumber, Double.valueOf(value));
    }

    @Override
    public void storeFloatField(int fieldNumber, float value) {
        storeObjectField(fieldNumber, Double.valueOf(value));
    }

    @Override
    public void storeIntField(int fieldNumber, int value) {
        storeObjectField(fieldNumber, Long.valueOf(value));
    }

    @Override
    public void storeLongField(int fieldNumber, long value) {
        storeObjectField(fieldNumber, Long.valueOf(value));
    }

    @Override
    public void storeShortField(int fieldNumber, short value) {
        storeObjectField(fieldNumber, Long.valueOf(value));
    }

    @Override
    public void storeStringField(int fieldNumber, String value) {
        storeObjectField(fieldNumber, value);
    }
}
