package fr.avianey.vhp;

import java.util.concurrent.atomic.AtomicInteger;

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

import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.util.Await;
import com.twitter.util.Future;
import com.twitter.util.TimeoutException;


public class MultiplyITCase {
    
    private ListeningServer targetServer; 
    private AtomicInteger hitCount;
    
    @Before
    public void startTargetServer() throws TimeoutException, InterruptedException {
        hitCount = new AtomicInteger();
        Service<HttpRequest, HttpResponse> target = new Service<HttpRequest, HttpResponse>() {
            @Override
            public Future<HttpResponse> apply(HttpRequest req) {
                hitCount.incrementAndGet();
                return Future.value((HttpResponse) new DefaultHttpResponse(
                        req.getProtocolVersion(), HttpResponseStatus.OK));
            }
            
        };
        targetServer = Http.serve(":9091", target);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                targetServer.close();
            }
        });
    }
    
    @After
    public void stopTargetServer() {
        targetServer.close();
    }
    
    @Test
    public void shouldReachTargetMultipleTimes() throws Exception {
        Await.result(Http.newService("localhost:9090").apply(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "nowhere")));
        Assert.assertEquals(3, hitCount.get());
    }

}
