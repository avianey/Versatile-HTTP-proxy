package fr.avianey.vhp;

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


public class ProxyITCase {
    
    private ListeningServer targetServer; 
    
    @Before
    public void startTargetServer() throws TimeoutException, InterruptedException {
        Service<HttpRequest, HttpResponse> target = new Service<HttpRequest, HttpResponse>() {
            @Override
            public Future<HttpResponse> apply(HttpRequest req) {
                return Future.value((HttpResponse) new DefaultHttpResponse(req.getProtocolVersion(), HttpResponseStatus.OK));
            }
            
        };
        targetServer = Http.serve(":8081", target);
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
    public void shouldReachTargetThroughProxy() throws Exception {
        Assert.assertEquals(HttpResponseStatus.OK, Await.result(Http.newService("localhost:8080").apply(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "nowhere"))).getStatus());
    }

}
