# Kerberos credentials

`kadmin:  ank -randkey +ok_to_auth_as_delegate HTTP/localhost`

`kadmin:  ktadd -k /home/zdv/Projects/test/spnego-test/http.keytab HTTP/localhost`

`kinit` obtains kerberos tgt ticket and stores it into the local cache

`klist` shows ticket information

`curl --negotiate -u : -b ~/curl-cookies.txt -c ~/curl-cookies.txt -v http://localhost:8080/actuator`

`kdestroy` destroys ticket


# Delegation

https://stackoverflow.com/questions/39743700/java-spnego-authentication-kerberos-constrained-delegation-kcd-to-backend-se

https://stackoverflow.com/questions/12529243/delegate-forward-kerberos-tickets-with-spring-security
