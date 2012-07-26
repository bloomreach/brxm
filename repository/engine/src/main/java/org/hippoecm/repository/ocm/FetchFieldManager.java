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
import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.ObjectProvider;
import org.datanucleus.store.fieldmanager.AbstractFieldManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FieldManager for retrieving field values from the JCR repository.
 */
public class FetchFieldManager extends AbstractFieldManager {

    protected final Logger log = LoggerFactory.getLogger(FetchFieldManager.class);

    private ObjectProvider op;
    private Session session;
    private Node node;
    private ColumnResolver columnResolver;
    private TypeResolver typeResolver;

    public FetchFieldManager(ObjectProvider op, Session session, ColumnResolver columnResolver, TypeResolver typeResolver, Node node) {
        this.op = op;
        this.session = session;
        this.columnResolver = columnResolver;
        this.typeResolver = typeResolver;
        this.node = node;
    }

    @Override
    public Object fetchObjectField(int fieldNumber) {
        AbstractMemberMetaData mmd = op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);

        AbstractMappingStrategy ms = AbstractMappingStrategy.findMappingStrategy(op, mmd, session, columnResolver, typeResolver);
        if (ms != null) {
            return ms.fetch();
        } else {
            // check for node name query
            if (".".equals(mmd.getColumn())) {
                // FIXME
            }
        }

        throw new NucleusDataStoreException("Cant obtain value for field " + mmd.getFullFieldName() + " since type="
                + mmd.getTypeName() + " is not supported for this datastore");
    }

    @Override
    public String fetchStringField(int fieldNumber) {
        return (String) fetchObjectField(fieldNumber);
    }

    @Override
    public boolean fetchBooleanField(int fieldNumber) {
        return (Boolean) fetchObjectField(fieldNumber);
    }

    @Override
    public byte fetchByteField(int fieldNumber) {
        return (Byte) fetchObjectField(fieldNumber);
    }

    @Override
    public char fetchCharField(int fieldNumber) {
        return (Character) fetchObjectField(fieldNumber);
    }

    @Override
    public double fetchDoubleField(int fieldNumber) {
        return (Double) fetchObjectField(fieldNumber);
    }

    @Override
    public float fetchFloatField(int fieldNumber) {
        return (Float) fetchObjectField(fieldNumber);
    }

    @Override
    public int fetchIntField(int fieldNumber) {
        return (Integer) fetchObjectField(fieldNumber);
    }

    @Override
    public long fetchLongField(int fieldNumber) {
        return (Long) fetchObjectField(fieldNumber);
    }

    @Override
    public short fetchShortField(int fieldNumber) {
        return (Short) fetchObjectField(fieldNumber);
    }
}
