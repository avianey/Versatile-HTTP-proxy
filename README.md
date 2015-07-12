# Versatile-HTTP-proxy

[![Build Status](https://travis-ci.org/avianey/Versatile-HTTP-proxy.svg?branch=master)](https://travis-ci.org/avianey/Versatile-HTTP-proxy)

#### Simple Proxy
Versatile HTTP proxy can act as a proxy.  
`vhp -l localhost:8080 -t www.example.com:80`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
```
#### Multiply requests
Versatile HTTP proxy can send request multiple times.  
`vhp -l localhost:8080 -t www.example.com:80 -m 3`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
                     |                     www.example.com:80
                     o--------------------+
                     |                     www.example.com:80
                     o--------------------+
```
The response is randomly picked between the multiple responses received and sent back to the client when all responses have been received.  

#### Multiple destinations (same request is sent to n targets)
Versatile HTTP proxy can send the request to multiple host:port.  
`vhp -p localhost:8080 -t www.example.com:80,www.example.org:80,www.example.net:80`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
                     |                     www.example.org:80
                     o--------------------+
                     |                     www.example.net:80
                     o--------------------+
```

#### Load balancing between destinations (randomly picked)
Versatile HTTP proxy can load balance requests to multiple host:port.  
`vhp -p localhost:8080 -t www.example.com:80,www.example.org:80,www.example.net:80 -b`
```
                     :8080                 www.example.com:80
+--------------------o-  -  -  -  -  -  - +
                     |                     www.example.org:80
                     o -  -  -  -  -  -  -+
                     |                     www.example.net:80
                     o  -  -  -  -  -  -  +
```
