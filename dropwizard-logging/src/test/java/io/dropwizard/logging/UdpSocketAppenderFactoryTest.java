package io.dropwizard.logging;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.validation.BaseValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class UdpSocketAppenderFactoryTest {

    private Thread thread;
    private CountDownLatch countDownLatch = new CountDownLatch(100);

    @Before
    public void setUp() throws Exception {
        thread = new Thread(() -> {
            try (DatagramSocket ss = new DatagramSocket(32144)) {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buffer = new byte[256];
                    readData(ss, buffer);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }

    private void readData(DatagramSocket socket, byte[] buffer) {
        try {
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(datagramPacket);
            System.out.print(new String(buffer, 0, datagramPacket.getLength()));
            countDownLatch.countDown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        thread.interrupt();
    }

    @Test
    public void testStartUdpAppender() throws Exception {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        objectMapper.getSubtypeResolver().registerSubtypes(UdpSocketAppenderFactory.class);

        YamlConfigurationFactory<DefaultLoggingFactory> factory = new YamlConfigurationFactory<>(
            DefaultLoggingFactory.class, BaseValidator.newValidator(), objectMapper, "dw");
        DefaultLoggingFactory defaultLoggingFactory = factory.build(
            new File(Resources.getResource("yaml/logging-udp.yml").toURI()));
        defaultLoggingFactory.configure(new MetricRegistry(), "udp-test");

        for (int i = 0; i < 100; i++) {
            LoggerFactory.getLogger("com.example.app").info("Application log {}", i);
        }
        countDownLatch.await();
    }
}
