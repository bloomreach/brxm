package org.hippoecm.hst.ocm;

/**
 * Interface to be implemented by beans that wish to be aware of the object converter.
 * 
 * @version $Id$
 */
public interface SimpleObjectConverterAware {

    /**
     * Callback that supplies the object converter.
     * 
     * @param simpleObjectConverter
     */
    void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter);
    
}
