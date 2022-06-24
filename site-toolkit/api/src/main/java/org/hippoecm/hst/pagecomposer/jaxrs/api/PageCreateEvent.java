/*
 * Copyright 2019-2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api;

/**
 * <p>
 *      <code>PageCreateEvent</code> which will be put on the internal Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link RuntimeException}
 *      through <code>PageCreateEvent#setException(java.lang.RuntimeException)</code>. When a {@link RuntimeException} is
 *      set on this <code>PageCreateEvent</code> by a listener, the
 *      org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent will rethrow the
 *      exception. The reason that this has to be done via this PageCreateEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 *  <p>
 *     <strong>Note</strong> that listeners for <code>PageCreateEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>PageCreateEvent</code> to the guava event bus
 * </p>
 */
public interface PageCreateEvent extends PageEvent<PageCreateContext> {

}
