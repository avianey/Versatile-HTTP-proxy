# Versatile-HTTP-proxy
Vesratile HTTP proxy can act as a proxy.  
`vht -p 8080 -t www.example.com:80`
```
                     :8080                 www.example.com:80
+--------------------+--------------------+
```

Vesratile HTTP proxy can send request multiple times.  
`vht -p 8080 -t www.example.com:80 -m 3`
```
                     :8080                 www.example.com:80
+--------------------+--------------------+
                     |                     www.example.com:80
                     +--------------------+
                     |                     www.example.com:80
                     +--------------------+
```

Vesratile HTTP proxy can send the request to multiple host:port.  
`vht -p 8080 -t www.example.com:80,www.example.org,www.example.net`
```
                     :8080                 www.example.com:80
+--------------------+--------------------+
                     |                     www.example.org:80
                     +--------------------+
                     |                     www.example.net:80
                     +--------------------+
```