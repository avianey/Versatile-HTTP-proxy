# Versatile-HTTP-proxy

[![Build Status](https://travis-ci.org/avianey/Versatile-HTTP-proxy.svg?branch=master)](https://travis-ci.org/avianey/Versatile-HTTP-proxy)

Versatile HTTP proxy can act as a proxy.  
`vhp -p 8080 -t www.example.com:80`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
```

Versatile HTTP proxy can send request multiple times.  
`vhp -p 8080 -t www.example.com:80 -m 3`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
                     |                     www.example.com:80
                     o--------------------+
                     |                     www.example.com:80
                     o--------------------+
``` 

Versatile HTTP proxy can send the request to multiple host:port.  
`vhp -p 8080 -t www.example.com:80,www.example.org:80,www.example.net:80`
```
                     :8080                 www.example.com:80
+--------------------o--------------------+
                     |                     www.example.org:80
                     o--------------------+
                     |                     www.example.net:80
                     o--------------------+
```

Versatile HTTP proxy can load balance requests to multiple host:port.  
`vhp -p 8080 -t www.example.com:80,www.example.org:80,www.example.net:80 -l`
```
                     :8080                 www.example.com:80
+--------------------o-  -  -  -  -  -  - +
                     |                     www.example.org:80
                     o -  -  -  -  -  -  -+
                     |                     www.example.net:80
                     o  -  -  -  -  -  -  +
```
