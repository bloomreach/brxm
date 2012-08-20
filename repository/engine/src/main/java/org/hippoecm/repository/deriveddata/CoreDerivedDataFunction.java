package org.hippoecm.repository.deriveddata;

import java.util.Map;

import javax.jcr.Value;

import org.hippoecm.repository.ext.DerivedDataFunction;

public class CoreDerivedDataFunction extends DerivedDataFunction {

    private static final long serialVersionUID = 1;

    @Override
    public Map<String, Value[]> compute(Map<String, Value[]> parameters) {
        return parameters;
    }
}
