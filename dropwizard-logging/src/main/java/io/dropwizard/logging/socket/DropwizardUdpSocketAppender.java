package io.dropwizard.logging.socket;

import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.recovery.ResilentUdpSocketOutputStream;
import ch.qos.logback.core.spi.DeferredProcessingAware;

public class DropwizardUdpSocketAppender<E extends DeferredProcessingAware> extends OutputStreamAppender<E> {

    private final String host;
    private final int port;

    public DropwizardUdpSocketAppender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void start() {
        setOutputStream(new ResilentUdpSocketOutputStream(host, port));
        super.start();
    }
}
