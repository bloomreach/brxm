/*
 * Copyright 2019-2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api;

/**
 * <p>
 *      <code>PageDeleteEvent</code> which will be put on the internal Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link RuntimeException}
 *      through <code>PageDeleteEvent#setException(java.lang.RuntimeException)</code>. When a {@link RuntimeException} is
 *      set on this <code>PageDeleteEvent</code> by a listener, the
 *      org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent will rethrow the
 *      exception. The reason that this has to be done via this PageDeleteEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 *  <p>
 *     <strong>Note</strong> that listeners for <code>PageDeleteEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>PageDeleteEvent</code> to the guava event bus
 * </p>
 */
public interface PageDeleteEvent extends PageEvent<PageDeleteContext> {

}
