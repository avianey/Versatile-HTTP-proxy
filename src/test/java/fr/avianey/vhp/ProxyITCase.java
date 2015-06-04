package fr.avianey.vhp;

import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.After;
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
                return Future.value((HttpResponse) new DefaultHttpResponse(
                        req.getProtocolVersion(), HttpResponseStatus.OK));
            }
            
        };
        targetServer = Http.serve(":8081", target);
        System.out.println("await");
        //Await.ready(targetServer);
        System.out.println("ready");
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                targetServer.close();
            }
        });
    }
    
    @After
    public void stopTargetServer() {
        System.out.println("closing");
        targetServer.close();
        System.out.println("closed");
    }
    
    @Test
    public void shouldReachTargetThroughProxy() {
        System.out.println("toto");
        System.out.println(Http.newService("localhost:8080").apply(new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "nowhere")).get().getStatus());
    }

}
