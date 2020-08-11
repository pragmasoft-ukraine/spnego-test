`kadmin:  ank -randkey HTTP/localhost`

`kadmin:  ktadd -k /home/zdv/Projects/test/spnego-test/http.keytab HTTP/localhost`

`kinit`

`klist`

`curl --negotiate -u : -b ~/curl-cookies.txt -c ~/curl-cookies.txt -v http://localhost:8080/actuator`

`kdestroy`