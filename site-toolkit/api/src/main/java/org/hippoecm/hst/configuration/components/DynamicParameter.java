/*
 * Copyright 2020-2022 Bloomreach
 */
package org.hippoecm.hst.configuration.components;

public interface DynamicParameter {

    /**
     * @return the name of the parameter used
     */
    String getName();

    /**
     * @return <code>true</code> if this is a required parameter 
     */    
    boolean isRequired();

    /**
     * @return the default value of this parameter
     */
    String getDefaultValue();

    /**
     * @return the displayName of this parameter. This can be the 'pretty' name for {@link #getName()}. If missing,
     * implementations can do a fallback to {@link #getName()}
     */
    String getDisplayName();

    /**
     * @return <code>true</code> if the parameter should not be shown in the channel manager UI
     */
    boolean isHideInChannelManager();

    /**
     * @return the type of the parameter
     */
    ParameterValueType getValueType();

    /**
     * @return <code>true</code> if this is a residual parameter 
     */
    boolean isResidual();

    /**
     * @return the parameter config of the parameter if present, {@code null} otherwise
     */
    DynamicParameterConfig getComponentParameterConfig();
}
