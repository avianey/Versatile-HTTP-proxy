/*
 * Copyright 2015 Antoine Vianey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.avianey.vhp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import scala.runtime.AbstractFunction1;

import com.twitter.finagle.Filter;
import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.util.Await;
import com.twitter.util.ConstFuture;
import com.twitter.util.Future;
import com.twitter.util.Return;

// TODO : suspend
// TODO : slow network (rate)
// TODO : compare responses
// TODO : record & replay
public class VersatileHttpProxy {

    private static final Map<String, Integer> OPTIONS_ORDER = new HashMap<>();
    static {
        int i = 0;
        OPTIONS_ORDER.put("l", i++);
        OPTIONS_ORDER.put("t", i++);
        OPTIONS_ORDER.put("m", i++);
        OPTIONS_ORDER.put("b", i++);
        OPTIONS_ORDER.put("sticky", i++);
        OPTIONS_ORDER.put("v", i++);
        OPTIONS_ORDER.put("w", i++);
        OPTIONS_ORDER.put("dump-requests", i++);
        OPTIONS_ORDER.put("dump-responses", i++);
    }

    public static void main(String[] args) throws Exception {
        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create the Options
        Options options = new Options();

        // listen port
        options.addOption(Option.builder("l")                          //
                                  .required()                          //
                                  .longOpt("listen")                   //
                                  .desc("host:port to listen on")      //
                                  .hasArg()                            //
                                  .argName("host:port")                //
                                  .build());                           //
        // target host:port
        options.addOption(Option.builder("t")                                                          //
                                  .required()                                                          //
                                  .longOpt("targeted-hosts")                                           //
                                  .valueSeparator(',')                                                 //
                                  .numberOfArgs(Option.UNLIMITED_VALUES)                               //
                                  .desc("comma separated list of host:port to forward requests to")    //
                                  .argName("targets")                                                  //
                                  .build());                                                           //
        options.addOption("b", "load-balancing", false, "load balance requests across hosts");
        options.addOption(Option.builder()                                                                                 //
                                  .longOpt("sticky")                                                                       //
                                  .desc("if load balancing is enabled, always use the same target for the same client")    //
                                  .build());                                                                               //
        // multiply >= 1
        options.addOption(Option.builder("m")                                                                   //
                                  .longOpt("multiply")                                                          //
                                  .desc("number of time to forward each request to each target, must be > 0")   //
                                  .hasArg()                                                                     //
                                  .argName("count")                                                             //
                                  .build());                                                                    //

        // wait mode
        options.addOption("w", "wait", false, "reply only when all responses received");

        // trace
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption(Option.builder()                              //
                                  .longOpt("dump-requests")             //
                                  .desc("dump http requests")           //
                                  .build());                            //
        options.addOption(Option.builder()                              //
                                  .longOpt("dump-responses")            //
                                  .desc("dump http responses")          //
                                  .build());                            //

        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            start(line);

            if (line.hasOption("sticky")) {
                System.err.println("sticky load balancing is not implemented yet");
            }
            if (line.hasOption("v")) {
                System.err.println("verbose mode is not implemented yet");
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(new Comparator<Option>() {
                @Override
                public int compare(Option o1, Option o2) {
                    String k1 = o1.getOpt() == null ? o1.getLongOpt() : o1.getOpt();
                    String k2 = o2.getOpt() == null ? o2.getLongOpt() : o2.getOpt();
                    return OPTIONS_ORDER.get(k1) - OPTIONS_ORDER.get(k2);
                }
            });
            formatter.printHelp("vhp -p <port> -t <targets>", options);
            System.exit(-1);
        }
    }

    @SuppressWarnings("unused")
    private static void start(CommandLine line) throws Exception {
        final boolean verbose = line.hasOption("v");
        final List<Service<HttpRequest, HttpResponse>> targets = targets(line);

        // initialize logic
        final Service<HttpRequest, List<HttpResponse>> logic = new ProxyService(
                line.hasOption("b"), 
                Integer.parseInt(line.getOptionValue("m", "1")), 
                targets);

        // start the proxy
        final ListeningServer proxy = Http.serve(line.getOptionValue("l"), 
                new Filter<HttpRequest, HttpResponse, HttpRequest, List<HttpResponse>>() {
                    @Override
                    public Future<HttpResponse> apply(HttpRequest request, Service<HttpRequest, List<HttpResponse>> service) {
                        return service.apply(request).flatMap(new AbstractFunction1<List<HttpResponse>, Future<HttpResponse>>() {
                            @Override
                            public Future<HttpResponse> apply(List<HttpResponse> responses) {
                                HttpResponse response = responses.get(ThreadLocalRandom.current().nextInt(responses.size()));
                                return new ConstFuture<HttpResponse>(new Return<HttpResponse>(response));
                            }
                        });
                    }
                }.andThen(logic)
        );
        Await.ready(proxy);

        // ensure correct shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                proxy.close();
                logic.close();
                for (Service<HttpRequest, HttpResponse> target : targets) {
                    target.close();
                }
                System.out.println("vhp stopped");
            }
        });
    }
    
    private static ArrayList<Service<HttpRequest, HttpResponse>> targets(CommandLine line) {
        String[] targetsDef = line.getOptionValues("t");
        final ArrayList<Service<HttpRequest, HttpResponse>> targets = new ArrayList<>();
        for (String target : targetsDef) {
            targets.add(Http.newService(target));
        }
        return targets;
    }
}
