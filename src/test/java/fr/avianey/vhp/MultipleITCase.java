package fr.avianey.vhp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

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


public class MultipleITCase {
    
    private Collection<ListeningServer> targetServers; 
    private Set<Integer> ports;
    
    @Before
    public void startTargetServers() throws TimeoutException, InterruptedException {
        ports = new java.util.HashSet<>();
        targetServers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int port = 9001 + i;
            targetServers.add(Http.serve(":" + port, new Service<HttpRequest, HttpResponse>() {
                @Override
                public Future<HttpResponse> apply(HttpRequest req) {
                    System.out.println(port);
                    ports.add(port);
                    return Future.value((HttpResponse) new DefaultHttpResponse(
                            req.getProtocolVersion(), HttpResponseStatus.OK));
                }
                
            }));
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                for (ListeningServer targetServer : targetServers) {
                    targetServer.close();
                }
            }
        });
    }
    
    @After
    public void stopTargetServers() {
        for (ListeningServer targetServer : targetServers) {
            targetServer.close();
        }
    }
    
    @Test
    public void shouldReachTargetMultipleTimes() throws Exception {
        Await.result(Http.newService("localhost:9000").apply(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "nowhere")));
        Assert.assertEquals(3, ports.size());
        Assert.assertTrue(ports.contains(9001));
        Assert.assertTrue(ports.contains(9002));
        Assert.assertTrue(ports.contains(9003));
    }

}
