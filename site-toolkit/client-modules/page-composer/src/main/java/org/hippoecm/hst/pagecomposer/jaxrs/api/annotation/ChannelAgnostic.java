package org.hippoecm.hst.pagecomposer.jaxrs.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * If added to a method, then the method is allowed regardless the specific channel being present on
 * {@link org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService} or no channel at all being present
 * on the {@link org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService}
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD})
public @interface ChannelAgnostic {
}