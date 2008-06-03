package org.hippoecm.frontend.plugins.reporting;

public interface ReportingNodeTypes {

    // NodeTypes
    public static final String NT_REPORT = "reporting:report";
    
    // Paths
    public static final String QUERY = "hippo:query";
    public static final String QUERY_STATEMENT = "hippo:query/jcr:statement";
    public static final String QUERY_LANGUAGE = "hippo:query/jcr:language";
    public static final String PARAMETER_NAMES = "reporting:parameternames";
    public static final String PARAMETER_VALUES = "reporting:parametervalues";
    public static final String LIMIT = "reporting:limit";
    public static final String OFFSET = "reporting:offset";
    public static final String PLUGIN = "frontend:plugin";
}
