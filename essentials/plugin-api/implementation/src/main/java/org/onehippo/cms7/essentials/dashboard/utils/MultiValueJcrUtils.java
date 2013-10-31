package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Common Utils for multi valued JCR properties.
 * @version "$Id$"
 */
public class MultiValueJcrUtils {


      /*
    *
    * Node property Utils
    *
    * */

    public static void deleteMultiValuePropertyValues(Node node, String propertyName, List<String> values) throws RepositoryException {
        for (String value : values) {
            deleteMultiValuePropertyValue(node, propertyName, value);
        }
    }

    public static void deleteMultiValuePropertyValue(Node node, String propertyName, String value) throws RepositoryException {
        List<String> values = getMultiValuePropertyValues(node, propertyName);
        if (values.contains(value)) {
            values.remove(value);
            updateMultiValueProperty(node, propertyName, values);
        }
    }

    public static void updateMultiValuePropertyValue(Node node, String propertyName, String oldValue, String newValue) throws RepositoryException {
        updateMultiValuePropertyValue(node, propertyName, oldValue, newValue, false);
    }

    public static void updateMultiValuePropertyValue(Node node, String propertyName, String oldValue, String newValue, boolean appendIfExists) throws RepositoryException {
        List<String> values = getMultiValuePropertyValues(node, propertyName);
        if (appendIfExists) {
            if (values.contains(oldValue)) {
                values.set(values.indexOf(oldValue), newValue);
                updateMultiValueProperty(node, propertyName, values);
            }
        } else {
            if (values.contains(oldValue) && !values.contains(newValue)) {
                values.set(values.indexOf(oldValue), newValue);
                updateMultiValueProperty(node, propertyName, values);
            } else {
                values.remove(oldValue);
                updateMultiValueProperty(node, propertyName, values);
            }
        }
    }

    public static boolean addMultiValuePropertyValue(Node node, String propertyName, String value) throws RepositoryException {
        return addMultiValuePropertyValue(node, propertyName, value, false);
    }

    public static boolean addMultiValuePropertyValue(Node node, String propertyName, String value, boolean checkExists, int index) throws RepositoryException {
        List<String> values = getMultiValuePropertyValues(node, propertyName);
        if (!checkExists || !values.contains(value)) {
            values.add(index, value);
            updateMultiValueProperty(node, propertyName, values);
            return true;
        }
        return false;
    }

    public static boolean addMultiValuePropertyValue(Node node, String propertyName, String value, boolean checkExists) throws RepositoryException {
        List<String> values = getMultiValuePropertyValues(node, propertyName);
        if (!checkExists || !values.contains(value)) {
            values.add(value);
            updateMultiValueProperty(node, propertyName, values);
            return true;
        }
        return false;
    }

    public static void addMultiValuePropertyValues(Node node, String propertyName, List<String> newValues) throws RepositoryException {
        addMultiValuePropertyValues(node, propertyName, newValues, false);
    }

    public static void addMultiValuePropertyValues(Node node, String propertyName, List<String> newValues, boolean checkExists) throws RepositoryException {
        List<String> values = getMultiValuePropertyValues(node, propertyName);
        for (String value : newValues) {
            if (!checkExists || !values.contains(value)) {
                values.add(value);
            }
        }
        updateMultiValueProperty(node, propertyName, values);
    }

    public static List<String> getMultiValuePropertyValues(Node node, String propertyName) throws RepositoryException {
        if (node.hasProperty(propertyName)) {
            Property prop = node.getProperty(propertyName);
            Value[] values = prop.getValues();
            List<String> valueList = new ArrayList<>(values.length);
            for (Value value : values) {
                valueList.add(value.getString());
            }
            return valueList;
        }
        return new ArrayList<>();
    }

    public static void updateMultiValueProperty(Node node, String propertyName, List<String> values) throws RepositoryException {
        node.setProperty(propertyName, values.toArray(new String[values.size()]));
    }

}
