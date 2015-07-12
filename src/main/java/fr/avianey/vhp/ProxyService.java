package fr.avianey.vhp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.twitter.finagle.Service;
import com.twitter.util.Future;
import com.twitter.util.Futures;

public class ProxyService extends Service<HttpRequest, List<HttpResponse>> {
    
    private final int multiply;
    private final boolean balance;
    private final List<Service<HttpRequest, HttpResponse>> targets;
    
    public ProxyService(boolean balance, int multiply, List<Service<HttpRequest, HttpResponse>> targets) {
        this.balance = balance;
        this.multiply = multiply;
        this.targets = targets;
    }

    @Override
    public Future<List<HttpResponse>> apply(HttpRequest request) {
        final List<Future<HttpResponse>> responses = new ArrayList<>();
        for (int i = 0; i < multiply; i++) {
            Collection<Service<HttpRequest, HttpResponse>> _targets; // ref to targets
            if (balance) {
                _targets = Collections.singletonList(targets.get(ThreadLocalRandom.current().nextInt(targets.size())));
            } else {
                _targets = targets;
            }
            for (Service<HttpRequest, HttpResponse> target : _targets) {
                responses.add(target.apply(request));
            }
        }
        return Futures.collect(responses);
    }
    
}
