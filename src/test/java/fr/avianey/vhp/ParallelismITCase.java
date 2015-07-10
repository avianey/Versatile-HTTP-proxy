package fr.avianey.vhp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.twitter.finagle.Http;
import com.twitter.finagle.netty3.numWorkers;
import com.twitter.util.Await;
import com.twitter.util.Awaitable;
import com.twitter.util.Duration;
import com.twitter.util.TimeoutException;

import fi.iki.elonen.NanoHTTPD;


public class ParallelismITCase {
    
    private static final int PARALLELISM = (int) numWorkers.apply(); // finagle default
    
    private NanoHTTPD server;
    private CyclicBarrier barrier = new CyclicBarrier(PARALLELISM);
    
    @Before
    public void startTargetServers() throws TimeoutException, InterruptedException, IOException {
        server = new NanoHTTPD(9091) {
            @Override
            public Response serve(IHTTPSession session) {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Throwables.propagate(e);
                }
                return new Response("");
            }
        };
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                server.stop();
            }
        });
    }
    
    @After
    public void stopTargetServers() {
        server.stop();
    }
    
    @Test
    public void shouldParallelizeRequests() throws Exception {
        List<Awaitable<?>> results = new ArrayList<>();
        for (int i = 0; i < PARALLELISM; i++) {
            results.add(Http.newService("localhost:9011").apply(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "nowhere")));
        }
        try {
            Await.all(results, new Duration(1_000_000 /* 1 second */));
        } catch (TimeoutException e) {
            Assert.fail(String.valueOf(barrier.getNumberWaiting()) + "/" + PARALLELISM);
            barrier.reset();
        }
    }

}
