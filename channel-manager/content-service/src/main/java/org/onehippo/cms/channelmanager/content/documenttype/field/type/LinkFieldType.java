package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LinkFieldType extends PrimitiveFieldType implements NodeFieldType {

    private static final Logger log = LoggerFactory.getLogger(LinkFieldType.class);
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    protected static final String[] PICKER_MULTIPLE_STRING_PROPERTIES = {
            "nodetypes",
    };
    @Override
    protected String getDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    protected void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues, final boolean validateValues) throws ErrorWithPayloadException {
        final String valueName = getId();
        final List<FieldValue> values = optionalValues.orElse(Collections.emptyList());

        if (validateValues) {
            checkCardinality(values);
        }

        try {
            final NodeIterator children = node.getNodes(valueName);
            FieldTypeUtils.writeNodeValues(children, values, getMaxValues(), this);
        } catch (final RepositoryException e) {
            log.warn("Failed to write image link field '{}'", valueName, e);
            throw new InternalServerErrorException();
        }
    }

    @Override
    protected int getPropertyType() {
        return PropertyType.STRING;
    }

    @Override
    protected List<FieldValue> readValues(final Node node) {
        final String nodeName = getId();

        try {
            final NodeIterator children = node.getNodes(nodeName);
            final List<FieldValue> values = new ArrayList<>((int) children.getSize());
            for (final Node child : new NodeIterable(children)) {
                final FieldValue value = readValue(child);
                if (value.hasValue()) {
                    values.add(value);
                }
            }
            return values;
        } catch (final RepositoryException e) {
            log.warn("Failed to read nodes for image link type '{}'", getId(), e);
        }
        return Collections.emptyList();
    }

    @Override
    public FieldValue readValue(final Node node) {
        final FieldValue value = new FieldValue();
        try {
            final String uuid = readUuid(node);
            value.setValue(uuid);
            value.setUrl(createUrl(uuid, node.getSession()));
        } catch (final RepositoryException e) {
            log.warn("Failed to read image link '{}' from node '{}'", getId(), JcrUtils.getNodePathQuietly(node), e);
        }
        return value;
    }

    protected abstract String createUrl(final String uuid, final Session session);

    private static String readUuid(final Node node) throws RepositoryException {
        final String uuid = JcrUtils.getStringProperty(node, HippoNodeType.HIPPO_DOCBASE, StringUtils.EMPTY);
        final String rootUuid = node.getSession().getRootNode().getIdentifier();
        return uuid.equals(rootUuid) ? StringUtils.EMPTY : uuid;
    }

    @Override
    public void writeValue(final Node node, final FieldValue fieldValue) throws RepositoryException {
        writeUuid(node, fieldValue.getValue());
    }

    private static void writeUuid(final Node node, final String uuid) throws RepositoryException {
        if (StringUtils.isEmpty(uuid)) {
            final String rootUuid = node.getSession().getRootNode().getIdentifier();
            writeDocBase(node, rootUuid);
        } else {
            writeDocBase(node, uuid);
        }
    }

    private static void writeDocBase(final Node node, final String rootUuid) throws RepositoryException {
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, rootUuid);
    }

    @Override
    public boolean validateValue(final FieldValue value) {
        return !isRequired() || validateSingleRequired(value);
    }

}
