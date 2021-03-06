package com.documents4j.ws.application;

import com.documents4j.api.IConverter;

import java.util.concurrent.TimeUnit;

/**
 * An implementation of this interface needs to be injected into
 * the resources of any {@link WebConverterApplication}.
 *
 * @see IWebConverterConfiguration
 */
public interface IWebConverterConfiguration {

    /**
     * The default network request timeout.
     */
    static final long DEFAULT_REQUEST_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    /**
     * Gets the underlying converter to which conversion requests are delegated.
     *
     * @return The underlying converter instance.
     */
    IConverter getConverter();

    /**
     * Gets the network request timeout in milliseconds.
     *
     * @return The network request timeout in milliseconds.
     */
    long getTimeout();
}
