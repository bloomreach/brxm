package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import org.onehippo.cms.services.validation.api.Violation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

class ValidatorTestUtils {

    private ValidatorTestUtils() {
    }

    static void assertValid(final Optional<Violation> violation) {
        assertFalse(violation.isPresent());
    }

    static void assertInvalid(final Optional<Violation> violation) {
        assertTrue(violation.isPresent());
    }

}
