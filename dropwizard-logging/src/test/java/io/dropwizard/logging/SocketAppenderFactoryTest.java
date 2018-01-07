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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

public class SocketAppenderFactoryTest {

    private Thread thread;
    private CountDownLatch countDownLatch = new CountDownLatch(100);

    @Before
    public void setUp() throws Exception {
        thread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(24562)) {
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket;
                    try {
                        socket = ss.accept();
                    } catch (SocketException e) {
                        break;
                    }
                    new Thread(() -> readData(socket)).start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
    }

    private void readData(Socket socket) {
        try (Socket s = socket; BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
            s.getInputStream()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
                countDownLatch.countDown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void tearDown() throws Exception {
        thread.interrupt();
    }

    @Test
    public void testStartSocketAppender() throws Exception {
        ObjectMapper objectMapper = Jackson.newObjectMapper();
        objectMapper.getSubtypeResolver().registerSubtypes(SocketAppenderFactory.class);

        YamlConfigurationFactory<DefaultLoggingFactory> factory = new YamlConfigurationFactory<>(
            DefaultLoggingFactory.class, BaseValidator.newValidator(), objectMapper, "dw");
        DefaultLoggingFactory defaultLoggingFactory = factory.build(new File(Resources.getResource("yaml/logging-tcp.yml").toURI()));
        defaultLoggingFactory.configure(new MetricRegistry(), "tcp-test");

        for (int i = 0; i < 100; i++) {
            LoggerFactory.getLogger("com.example.app").info("Application log {}", i);
        }
        countDownLatch.await();
    }
}
