package org.onehippo.cms7.channelmanager.templatecomposer.deviceskins;

/**
 * The basic model interface for the device manager
 *
 * @version "$Id$"
 */
public interface StyleableDevice {

    /**
     * @return The CSS style for the iframe
     */
    public String getStyle();

    /**
     * @return The CSS style for the wrapper of the iframe
     */
    public String getWrapStyle();

    /**
     * @return Name which pops up in the combobox.
     */
    public String getName();

    /**
     * @return Identifier which is saved on the channel.
     */
    public String getId();

    /**
     * @param name Able to set a translatable name.
     */
    public void setName(String name);


}
