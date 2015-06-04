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

import java.util.*;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.google.common.collect.ImmutableList;
import com.twitter.finagle.Http;
import com.twitter.finagle.ListeningServer;
import com.twitter.finagle.Service;
import com.twitter.util.Await;
import com.twitter.util.Future;

// TODO : suspend
// TODO : slow network (rate)
// TODO : compare responses
// TODO : record & replay
public class VersatileHttpProxy {

    private static final Map<String, Integer> OPTIONS_ORDER = new HashMap<>();
    static {
        int i = 0;
        OPTIONS_ORDER.put("p", i++);
        OPTIONS_ORDER.put("t", i++);
        OPTIONS_ORDER.put("m", i++);
        OPTIONS_ORDER.put("b", i++);
        OPTIONS_ORDER.put("sticky", i++);
        OPTIONS_ORDER.put("v", i++);
        OPTIONS_ORDER.put("dump-requests", i++);
        OPTIONS_ORDER.put("dump-responses", i++);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) throws Exception {
        // create the command line parser
        CommandLineParser parser = new BasicParser();

        // create the Options
        Options options = new Options();

        // listen port
        options.addOption(OptionBuilder                                    //
                                  .isRequired()                            //
                                  .withLongOpt("listen-port")              //
                                  .withDescription("port to listen on")    //
                                  .hasArg()                                //
                                  .withArgName("port")                     //
                                  .create("p"));                           //
        // target host:port
        options.addOption(OptionBuilder                                                                           //
                                  .isRequired()                                                                   //
                                  .withLongOpt("targeted-hosts")                                                  //
                                  .withValueSeparator(',')                                                        //
                                  .withDescription("comma separated list of host:port to forward requests to")    //
                                  .hasArg()                                                                       //
                                  .withArgName("targets")                                                         //
                                  .create("t"));                                                                  //
        options.addOption("b", "load-balancing", false, "load balance requests across hosts");
        options.addOption(OptionBuilder                                                                                               //
                                  .withLongOpt("sticky")                                                                              //
                                  .withDescription("if load balancing is enabled, always use the same target for the same client")    //
                                  .create());                                                                                         //
        // multiply >= 1
        options.addOption(OptionBuilder                                                                                    //
                                  .withLongOpt("multiply")                                                                 //
                                  .withDescription("number of time to forward each request to each target, must be > 0")   //
                                  .hasArg()                                                                                //
                                  .withArgName("count")                                                                    //
                                  .create("m"));                                                                           //
        // trace
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption(OptionBuilder                                     //
                                  .withLongOpt("dump-requests")             //
                                  .withDescription("dump http requests")    //
                                  .create());                               //
        options.addOption(OptionBuilder                                     //
                                  .withLongOpt("dump-responses")            //
                                  .withDescription("dump http responses")   //
                                  .create());                               //

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
            formatter.printHelp("hdt -p <port> -t <targets>", options);
            System.exit(-1);
        }
    }

    @SuppressWarnings("unused")
    private static void start(CommandLine line) throws Exception {
        // initialize clients for each target
        String[] targetsDef = line.getOptionValues("t");
        final ArrayList<Service<HttpRequest, HttpResponse>> targets = new ArrayList<>();
        for (String target : targetsDef) {
            targets.add(Http.newService(target));
        }

        // initialize logic
        final boolean verbose = line.hasOption("v");
        final boolean balance = line.hasOption("b");
        final int multiply = Integer.parseInt(line.getOptionValue("m", "1"));
        final Service<HttpRequest, HttpResponse> logic = new Service<HttpRequest, HttpResponse>() {
            @Override
            public Future<HttpResponse> apply(HttpRequest request) {
                Collection<Service<HttpRequest, HttpResponse>> _targets; // ref to targets
                if (balance) {
                    _targets = ImmutableList.of(targets.get((int) (Math.random() * targets.size())));
                } else {
                    _targets = targets;
                }
                Future<HttpResponse> response = null;
                for (int i = 0; i < multiply; i++) {
                    for (Service<HttpRequest, HttpResponse> target : _targets) {
                        response = target.apply(request);
                        // for output :
                        // ((Response) response.get()).getContentString()
                    }
                }
                return response;
            }
        };

        // start the proxy
        int port = Short.parseShort(line.getOptionValue("p"));
        final ListeningServer proxy = Http.serve(":" + port, logic);
        Await.ready(proxy);

        // ensure correct shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                proxy.close();
                for (Service<HttpRequest, HttpResponse> target : targets) {
                    target.close();
                }
                System.out.println("vhp stopped");
            }
        });
    }
}
