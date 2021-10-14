package org.hippoecm.frontend.validation;


import java.util.Set;

import lombok.Builder;
import lombok.Singular;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@ToString
public class SvgValidationResult {

    @Singular
    Set<String> offendingElements;
    @Singular
    Set<String> offendingAttributes;

}
