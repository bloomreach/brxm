/*
 * Copyright 2018-2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.jaxrs.api;

/**
 * <p>
 *      <code>PageCopyEvent</code> which will be put on the internal Guava event bus for
 *      <code>synchronous</code> events dispatching where listeners to this event can inject logic or short-circuit
 *      processing by setting a {@link java.lang.RuntimeException}
 *      through <code>PageCopyEvent#setException(java.lang.RuntimeException)</code>. When a {@link java.lang.RuntimeException} is
 *      set on this <code>PageCopyEvent</code> by a listener, the
 *      org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource#publishSynchronousEvent will rethrow the
 *      exception. The reason that this has to be done via this PageCopyEvent object is that Guava
 *      {@link com.google.common.eventbus.EventBus} always catches an exception thrown by a listener, even when injecting a
 *      custom {@link com.google.common.eventbus.SubscriberExceptionHandler}
 * </p>
 *  <p>
 *     <strong>Note</strong> that listeners for <code>PageCopyEvent</code>s must <strong>never</strong> invoke
 *     {@link javax.jcr.Session#save() HstRequestContext#getSession()#save()}. Changes in the JCR {@link javax.jcr.Session}
 *     will always be persisted by the code that posted the <code>PageCopyEvent</code> to the guava event bus
 * </p>
 */
public interface PageCopyEvent extends PageEvent<PageCopyContext> {

    /**
     * Return the page copy context data.
     * @return the page copy context data
     * @deprecated use {@link #getPageActionContext()}
     */
    @Deprecated
    public PageCopyContext getPageCopyContext();

}
