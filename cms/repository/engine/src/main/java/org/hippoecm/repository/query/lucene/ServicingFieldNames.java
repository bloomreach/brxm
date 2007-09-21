package org.hippoecm.repository.query.lucene;

public class ServicingFieldNames {
    
    /**
     * Private constructor.
     */
    private ServicingFieldNames() {
    }
    
    /**
     * Prefix for all field names that are facet properties.
     */
    public static final String HIPPO_FACET = "HIPPOFACET:".intern();

    /**
     * Prefix for all field names that are path properties.
     */
    public static final String HIPPO_PATH = "_:HIPPOPATH:".intern();
    

    /**
     * Prefix for all field names that are depth properties.
     */
    public static final String HIPPO_DEPTH = "_:HIPPODEPTH:".intern();
    
    /**
     * Name of the field that contains all available properties that are available
     * for this indexed node. 
     */
    public static final String FACET_PROPERTIES_SET = "_:FACET_PROPERTIES_SET".intern(); 
    
}
