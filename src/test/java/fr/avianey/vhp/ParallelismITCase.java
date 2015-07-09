package fr.avianey.vhp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.finagle.netty3.numWorkers;
import com.twitter.util.Await;
import com.twitter.util.Awaitable;
import com.twitter.util.Duration;
import com.twitter.util.Future;
import com.twitter.util.TimeoutException;


public class ParallelismITCase {
    
    private static final int PARALLELISM = (int) numWorkers.apply(); // finagle default
    
    private ListeningServer target;
    private CyclicBarrier barrier = new CyclicBarrier(PARALLELISM);
    
    @Before
    public void startTargetServers() throws TimeoutException, InterruptedException {
        target = Http.serve(":9011", new Service<HttpRequest, HttpResponse>() {
            @Override
            public Future<HttpResponse> apply(HttpRequest req) {
                try {
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Throwables.propagate(e);
                }
                return Future.value((HttpResponse) new DefaultHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK));
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                target.close();
            }
        });
    }
    
    @After
    public void stopTargetServers() {
        target.close();
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
