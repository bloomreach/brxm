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
package org.hippoecm.hst.configuration.channel;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelPropertyMapper {

    static final Logger log = LoggerFactory.getLogger(ChannelPropertyMapper.class);

    private ChannelPropertyMapper() {
    }

    public static Channel readChannel(HstNode channelNode) {
        return readChannel(channelNode, channelNode.getName());
    }

    static Channel readChannel(HstNode channelNode, String channelId) {
        Channel channel = new Channel(channelId);
        channel.setName(channelNode.getName());
        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME)) {
            channel.setName(channelNode.getValueProvider().getString(HstNodeTypes.CHANNEL_PROPERTY_NAME));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE)) {
            channel.setType(channelNode.getValueProvider().getString(HstNodeTypes.CHANNEL_PROPERTY_TYPE));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.CHANNEL_PROPERTY_DEFAULT_DEVICE)) {
            channel.setDefaultDevice(channelNode.getValueProvider().getString(HstNodeTypes.CHANNEL_PROPERTY_DEFAULT_DEVICE));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.CHANNEL_PROPERTY_DEVICES)) {
            final String[] devices = channelNode.getValueProvider().getStrings(HstNodeTypes.CHANNEL_PROPERTY_DEVICES);
            channel.setDevices(Arrays.asList(devices));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            channel.setChannelNodeLockedBy(channelNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY));
        }
        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY)) {
            channel.setLastModifiedBy(channelNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
            channel.setLastModified(channelNode.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            channel.setLockedOn(channelNode.getValueProvider().getDate(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON));
        }

        if (channelNode.getValueProvider().hasProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS)) {
            String className = channelNode.getValueProvider().getString(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS);
            try {
                Class clazz = ChannelPropertyMapper.class.getClassLoader().loadClass(className);
                if (!ChannelInfo.class.isAssignableFrom(clazz)) {
                    log.warn("Class " + className + " does not extend ChannelInfo");
                    return channel;
                }
                channel.setChannelInfoClassName(className);
                HstNode channelInfoNode = channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
                if (channelInfoNode != null) {
                    Map<String, Object> properties = channel.getProperties();
                    List<HstPropertyDefinition> propertyDefinitions = ChannelInfoClassProcessor.getProperties(clazz);
                    Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(channelInfoNode, propertyDefinitions);
                    for (HstPropertyDefinition def : propertyDefinitions) {
                        if (values.get(def) != null) {
                            properties.put(def.getName(), values.get(def));
                        } else if (def.getDefaultValue() != null) {
                            properties.put(def.getName(), def.getDefaultValue());
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                log.warn("Could not load channel info class " + className + " for channel " + channel.getId(), e);
            }
        }
        return channel;
    }

    private static void savePropertyOrRemoveIfNull(Node node, String propertyName, String value) throws RepositoryException {
        if (value != null) {
            node.setProperty(propertyName, value);
        } else if (node.hasProperty(propertyName)) {
            node.getProperty(propertyName).remove();
        }
    }

    public static void saveChannel(Node channelNode, Channel channel) throws RepositoryException, ChannelException {
        long validateLastModifiedTimestamp = -1L;
        if (channel.getLastModified() != null) {
            validateLastModifiedTimestamp = channel.getLastModified().getTimeInMillis();
        }
        tryLockOnNodeIfNeeded(channelNode, validateLastModifiedTimestamp);
        savePropertyOrRemoveIfNull(channelNode, HstNodeTypes.CHANNEL_PROPERTY_NAME, channel.getName());
        savePropertyOrRemoveIfNull(channelNode, HstNodeTypes.CHANNEL_PROPERTY_TYPE, channel.getType());
        savePropertyOrRemoveIfNull(channelNode, HstNodeTypes.CHANNEL_PROPERTY_DEFAULT_DEVICE, channel.getDefaultDevice());

        if (channel.getDevices() != null) {
            channelNode.setProperty(HstNodeTypes.CHANNEL_PROPERTY_DEVICES, channel.getDevices().toArray(new String[0]));
        } else if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_DEVICES)) {
            channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_DEVICES).remove();
        }

        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName != null) {
            channelNode.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, channelInfoClassName);

            Node channelPropsNode;
            if (!channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
                channelPropsNode = channelNode.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
            } else {
                channelPropsNode = channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
            }
            try {
                Class<? extends ChannelInfo> channelInfoClass = (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(channelInfoClassName);
                ChannelPropertyMapper.saveProperties(channelPropsNode, ChannelInfoClassProcessor.getProperties(channelInfoClass), channel.getProperties());
            } catch (ClassNotFoundException e) {
                log.warn("Could not find channel info class " + channelInfoClassName, e);
                throw new IllegalStateException("Could not find channel info class ", e);
            }
        } else {
            if (channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
                channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO).remove();
            }
        }
    }

    static Map<HstPropertyDefinition, Object> loadProperties(HstNode channelInfoNode, List<HstPropertyDefinition> propertyDefinitions) {
        Map<HstPropertyDefinition, Object> properties = new HashMap<>();
        if (propertyDefinitions != null) {
            for (HstPropertyDefinition pd : propertyDefinitions) {
                Object value = null;
                if (channelInfoNode.getValueProvider().hasProperty(pd.getName())) {
                    Object property = channelInfoNode.getValueProvider().getProperties().get(pd.getName());
                    value = getHstValueFromObject(pd, property);
                }
                properties.put(pd, value);
            }
        } else {
            for (Map.Entry<String, Object> property : channelInfoNode.getValueProvider().getProperties().entrySet()) {
                AbstractHstPropertyDefinition hpd = new AbstractHstPropertyDefinition(property.getKey()) {};
                properties.put(hpd, getHstValueFromObject(hpd, property));
            }
        }
        return properties;
    }

    static void saveProperties(Node node, List<HstPropertyDefinition> definitions, Map<String, Object> properties) throws RepositoryException {
        for (PropertyIterator propertyIterator = node.getProperties(); propertyIterator.hasNext(); ) {
            Property prop = propertyIterator.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            prop.remove();
        }
        for (HstPropertyDefinition definition : definitions) {
            if (properties.containsKey(definition.getName()) && properties.get(definition.getName()) != null) {
                setHstValueToJcr(node, definition, properties.get(definition.getName()));
            }
        }
    }

    private static Object getHstValueFromObject(final HstPropertyDefinition pd, final Object property) {
        Object value;
        if (property.getClass().isArray()) {
            List<Object> valueList = (List<Object>)(value =  new LinkedList());
            for (Object propVal : (Object[])property) {
                if (correctType(propVal, pd.getValueType())) {
                    valueList.add(propVal);
                } else {
                    valueList.add(pd.getDefaultValue());
                }
            }
        } else {
            if (correctType(property, pd.getValueType())) {
                value = property;
            } else {
                value = pd.getDefaultValue();
            }
        }
        return value;

    }

    private static boolean correctType(final Object property, final HstValueType valueType) {
        switch (valueType) {
            case STRING:
                return property instanceof String;
            case BOOLEAN:
                return property instanceof Boolean;
            case DATE:
                return property instanceof Calendar;
            case DOUBLE:
                return property instanceof Double;
            case INTEGER:
                // fall through: JCR does not support int but long, so return a long
                 return property instanceof Long;
            case LONG:
                return property instanceof Long;
            default:
                return false;
        }
    }

    private static void setHstValueToJcr(Node node, HstPropertyDefinition propDef, Object value) throws RepositoryException {
        ValueFactory vf = node.getSession().getValueFactory();
        if (value instanceof List) {
            Value[] values = new Value[((List) value).size()];
            int i = 0;
            for (Object val : (List) value) {
                values[i++] = javaToJcr(vf, val, propDef);
            }
            node.setProperty(propDef.getName(), values);
        } else {
            node.setProperty(propDef.getName(), javaToJcr(vf, value, propDef));
        }
    }

    private static Value javaToJcr(ValueFactory vf, Object value, HstPropertyDefinition propDef) throws RepositoryException {
        if (value instanceof String) {
            if (propDef.getValueType() != HstValueType.STRING) {
                log.warn("Cannot store a String '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue((String) value);
            }
        } else if (value instanceof Boolean) {
            if (propDef.getValueType() != HstValueType.BOOLEAN) {
                log.warn("Cannot store a Boolean '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue((Boolean) value);
            }
        } else if (value instanceof Integer) {
            if (propDef.getValueType() != HstValueType.INTEGER) {
                log.warn("Cannot store a Integer (Long in jcr) '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue(((Integer)value).longValue());
            }
        } else if (value instanceof Long) {
            if (propDef.getValueType() != HstValueType.LONG) {
                log.warn("Cannot store a Long '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue((Long) value);
            }
        } else if (value instanceof Double) {
            if (propDef.getValueType() != HstValueType.DOUBLE) {
                log.warn("Cannot store a Double '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue((Double) value);
            }
        } else if (value instanceof Calendar) {
            if (propDef.getValueType() != HstValueType.DATE) {
                log.warn("Cannot store a Calendar '{}' for '{}'. Store default value instead", value, propDef.getName());
                return defaultValueToJcr(vf, propDef);
            } else {
                return vf.createValue((Calendar) value);
            }
        } else {
            throw new RepositoryException("Unable to find valid value type for " + value);
        }
    }

    private static Value defaultValueToJcr(ValueFactory vf, HstPropertyDefinition propDef) throws RepositoryException {
        switch (propDef.getValueType()) {
            case STRING:
                if (!(propDef.getDefaultValue() instanceof String)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue("");
                }
                return vf.createValue((String) propDef.getDefaultValue());
            case BOOLEAN:
                if (!(propDef.getDefaultValue() instanceof Boolean)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue(false);
                }
                return vf.createValue((Boolean) propDef.getDefaultValue());
            case DATE:
                if (!(propDef.getDefaultValue() instanceof Calendar)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue(Calendar.getInstance());
                }
                return vf.createValue((Calendar) propDef.getDefaultValue());
            case DOUBLE:
                if (!(propDef.getDefaultValue() instanceof Double)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue(0D);
                }
                return vf.createValue((Double) propDef.getDefaultValue());
            case INTEGER:
                if (!(propDef.getDefaultValue() instanceof Integer)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue(0L);
                }
                return vf.createValue(((Integer)propDef.getDefaultValue()).longValue());
            case LONG:
                if (!(propDef.getDefaultValue() instanceof Long)) {
                    log.warn("HstPropertyDefinition Default value '{}' incompatible with HstPropertyDefinition type '{}'. Return default value for type", propDef.getDefaultValue(), propDef.getValueType());
                    return vf.createValue(0L);
                }
                return vf.createValue((Long)propDef.getDefaultValue());
            default:
                throw new RuntimeException("Unexpected HstValueType " + propDef.getDefaultValue().getClass().getName());
        }
    }

    public static  boolean isLockedBySomeoneElse(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return !configurationNode.getSession().getUserID().equals(holder);
    }

    public static  boolean isLockedBySession(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return configurationNode.getSession().getUserID().equals(holder);
    }

    public static  String getLockedBy(Node configurationNode) throws RepositoryException {
        if (!configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            return null;
        }
        return configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
    }

    /**
     * tries to set a lock. If there is not yet a lock, then also
     * a timestamp validation is done whether the configuration node that needs to be locked has not been modified
     * by someone else
     *
     */
    public static void tryLockOnNodeIfNeeded(final Node nodeToLock, final long validateLastModifiedTimestamp) throws RepositoryException, ChannelException {
        Session session = nodeToLock.getSession();
        if (isLockedBySomeoneElse(nodeToLock)) {
            log.info("Node '{}' is already locked by someone else.", nodeToLock.getPath());
            throw new ChannelException("Node '"+nodeToLock.getPath()+"' is already locked by someone else.");
        }
        if (isLockedBySession(nodeToLock)) {
            log.debug("Container '{}' already has a lock for user '{}'.", nodeToLock.getPath(), session.getUserID());
            return;
        }

        if (nodeToLock.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
            long existingTimeStamp = nodeToLock.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
            if (existingTimeStamp != validateLastModifiedTimestamp) {
                Calendar existing = Calendar.getInstance();
                existing.setTimeInMillis(existingTimeStamp);
                Calendar validate = Calendar.getInstance();
                validate.setTimeInMillis(validateLastModifiedTimestamp);
                DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss:SSS zzz", Locale.US);
                log.info("Node '{}' has been modified at '{}' but validation timestamp was '{}'. Cannot acquire lock now for user '{}'.",
                        new String[]{nodeToLock.getPath(), dateFormat.format(existing.getTime()),
                                dateFormat.format(validate.getTime()) , session.getUserID()});
                throw new ChannelException("Node '"+nodeToLock.getPath()+"' cannot be changed because timestamp validation did not pass.");
            }
        }
        log.info("Node '{}' gets a lock for user '{}'.", nodeToLock.getPath(), session.getUserID());
        nodeToLock.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        Calendar now = Calendar.getInstance();
        if (!nodeToLock.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            nodeToLock.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
        }
        nodeToLock.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
        nodeToLock.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
    }

}
