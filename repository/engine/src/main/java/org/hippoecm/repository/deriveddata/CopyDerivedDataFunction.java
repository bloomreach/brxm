package org.hippoecm.repository.deriveddata;

import java.util.Map;

import javax.jcr.Value;

import org.hippoecm.repository.ext.DerivedDataFunction;

public class CopyDerivedDataFunction extends DerivedDataFunction {
    private static final long serialVersionUID = 1;

    @Override
    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        Value[] source = parameters.get("source");
        if (source != null) {
            Value[] destination = new Value[source.length];
            System.arraycopy(source, 0, destination, 0, source.length);
            parameters.put("destination", destination);
        }
        return parameters;
    }
}
