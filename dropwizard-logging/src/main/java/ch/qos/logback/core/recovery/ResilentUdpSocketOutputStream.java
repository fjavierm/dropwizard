package ch.qos.logback.core.recovery;

import ch.qos.logback.core.net.SyslogOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResilentUdpSocketOutputStream extends ResilientOutputStreamBase {

    private final String host;
    private final int port;

    public ResilentUdpSocketOutputStream(String host, int port) {
        this.host = host;
        this.port = port;
        try {
            super.os = openNewOutputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create a TCP connection to " + host + ":" + port, e);
        }
        this.presumedClean = true;
    }

    @Override
    String getDescription() {
        return "udp [" + host + ":" + port + "]";
    }

    @Override
    OutputStream openNewOutputStream() throws IOException {
        return new SyslogOutputStream(host, port);
    }
}
