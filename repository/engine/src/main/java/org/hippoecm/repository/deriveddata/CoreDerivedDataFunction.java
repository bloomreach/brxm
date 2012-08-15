package org.hippoecm.repository.deriveddata;

import java.util.Map;

import javax.jcr.Value;

import org.hippoecm.repository.ext.DerivedDataFunction;

public class CoreDerivedDataFunction extends DerivedDataFunction {
    static final long serialVersionUID = 1;

    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        return parameters;
    }
}
