/*
 *  Copyright 2011 Hippo.
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelPropertyMapper {

    static final Logger log = LoggerFactory.getLogger(ChannelPropertyMapper.class);

    private ChannelPropertyMapper() {
    }

    public static Channel readChannel(Node channelNode) throws RepositoryException {
        return readChannel(channelNode, channelNode.getName());
    }

    static Channel readChannel(Node channelNode, String channelId) throws RepositoryException {
        Channel channel = new Channel(channelId);
        channel.setName(channelNode.getName());
        if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME)) {
            channel.setName(channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME).getString());
        }

        if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE)) {
            channel.setType(channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE).getString());
        }

        if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS)) {
            String className = channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS).getString();
            try {
                Class clazz = ChannelPropertyMapper.class.getClassLoader().loadClass(className);
                if (!ChannelInfo.class.isAssignableFrom(clazz)) {
                    log.warn("Class " + className + " does not extend ChannelInfo");
                    return channel;
                }
                channel.setChannelInfoClassName(className);
                if (channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
                    Map<String, Object> properties = channel.getProperties();
                    List<HstPropertyDefinition> propertyDefinitions = ChannelInfoClassProcessor.getProperties(clazz);
                    Map<HstPropertyDefinition, Object> values = ChannelPropertyMapper.loadProperties(channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO), propertyDefinitions);
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

    public static void saveChannel(Node channelNode, Channel channel) throws RepositoryException {
        if (channel.getName() != null) {
            channelNode.setProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME, channel.getName());
        } else if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME)) {
            channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME).remove();
        }
        if (channel.getType() != null) {
            channelNode.setProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE, channel.getType());
        } else if (channelNode.hasProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE)) {
            channelNode.getProperty(HstNodeTypes.CHANNEL_PROPERTY_TYPE).remove();
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
                log.error("Could not find channel info class " + channelInfoClassName, e);
            }
        } else {
            if (channelNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
                channelNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO).remove();
            }
        }

    }

    static Map<HstPropertyDefinition, Object> loadProperties(Node channelInfoNode, List<HstPropertyDefinition> propertyDefinitions) throws RepositoryException {
        Map<HstPropertyDefinition, Object> properties = new HashMap<HstPropertyDefinition, Object>();
        if (propertyDefinitions != null) {
            for (HstPropertyDefinition pd : propertyDefinitions) {
                Object value = null;
                if (channelInfoNode.hasProperty(pd.getName())) {
                    Property property = channelInfoNode.getProperty(pd.getName());
                    value = getHstValueFromJcr(pd, property);
                }
                properties.put(pd, value);
            }
        } else {
            for (PropertyIterator propertyIterator = channelInfoNode.getProperties(); propertyIterator.hasNext(); ) {
                Property prop = propertyIterator.nextProperty();
                if (prop.getDefinition().isProtected()) {
                    continue;
                }
                HstPropertyDefinition hpd = new JcrHstPropertyDefinition(prop, false);
                properties.put(hpd, getHstValueFromJcr(hpd, prop));
            }
        }
        return properties;
    }

    static void saveProperties(Node mountNode, List<HstPropertyDefinition> definitions, Map<String, Object> properties) throws RepositoryException {
        for (PropertyIterator propertyIterator = mountNode.getProperties(); propertyIterator.hasNext(); ) {
            Property prop = propertyIterator.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            prop.remove();
        }
        for (HstPropertyDefinition definition : definitions) {
            if (properties.containsKey(definition.getName()) && properties.get(definition.getName()) != null) {
                setHstValueToJcr(mountNode, definition, properties.get(definition.getName()));
            }
        }
    }

    private static Object getHstValueFromJcr(final HstPropertyDefinition pd, final Property property) throws RepositoryException {
        Object value;
        if (property.isMultiple()) {
            List values = (List) (value = new LinkedList());
            for (Value jcrValue : property.getValues()) {
                try {
                    values.add(jcrToJava(jcrValue, pd.getValueType()));
                } catch (ValueFormatException e) {
                    log.warn("Invalid channel property value '{}' found for property '{}'. Using default value", value.toString(), pd.getName());
                    values.add(pd.getDefaultValue());
                }
            }
        } else {
            try {
                value = jcrToJava(property.getValue(), pd.getValueType());
            } catch (ValueFormatException e) {
                log.warn("Invalid channel property value '{}' found for property '{}'. Using default value", property.getValue().toString(), pd.getName());
                value = pd.getDefaultValue();
            }
        }
        return value;
    }

    public static Object jcrToJava(final Value value, final HstValueType type) throws ValueFormatException, RepositoryException {
        if (type == null) {
            switch (value.getType()) {
                case PropertyType.STRING:
                    return value.getString();
                case PropertyType.BOOLEAN:
                    return value.getBoolean();
                case PropertyType.DATE:
                    return value.getDate();
                case PropertyType.LONG:
                    return value.getLong();
                case PropertyType.DOUBLE:
                    return value.getDouble();
            }
            return null;
        }
        switch (type) {
            case STRING:
                return value.getString();
            case BOOLEAN:
                return value.getBoolean();
            case DATE:
                return value.getDate();
            case DOUBLE:
                return value.getDouble();
            case INTEGER:
                return value.getLong();
            default:
                return null;
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
                return null;
        }
    }
}
