package org.hippoecm.hst.configuration.components;

/**
 * ParameterType used to provide a hint to the template composer about the type of the parameter. This is just a
 * convenience interface that provides some constants for the field types.
 * @deprecated use the return type plus additional annotations
 */
@Deprecated
public interface ParameterType {
    String STRING = "STRING";
    String NUMBER = "NUMBER";
    String BOOLEAN = "BOOLEAN";
    String DATE = "DATE";
    String COLOR = "COLOR";
    String DOCUMENT = "DOCUMENT";
}