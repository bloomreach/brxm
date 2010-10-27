package org.hippoecm.hst.pagecomposer.rest;

/**
 * ParameterType used to provide a hint to the pagecomposer about the type of the parameter.
 * This is just a convenience interface that provides some constants for the field types.
 */
public interface ParameterType {
    String STRING = "STRING";
    String NUMBER = "NUMBER";
    String BOOLEAN = "BOOLEAN";
    String DATE = "DATE";
    String COLOR = "COLOR";
}
