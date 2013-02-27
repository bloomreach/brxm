package org.onehippo.repository.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on implementations of {@link DaemonModule}s to
 * inform the system about which services it uses from the
 * {@link org.onehippo.cms7.services.HippoServiceRegistry}
 * and which are provided by other {@link DaemonModule}s.
 * Together with the {@link ProvidesService} annotation, this annotation
 * determines the order in which modules are started.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RequiresService {
    Class<?>[] types();
}
