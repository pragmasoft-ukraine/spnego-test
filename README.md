# Kerberos credentials

`kadmin:  ank -randkey +ok_to_auth_as_delegate HTTP/localhost`

`kadmin:  ktadd -k http.keytab HTTP/localhost`

`kinit` obtains kerberos tgt ticket and stores it into the local cache

`kvno -k http.keytab -U zdv -P HTTP/krb5.local postgres/krb5.local`

`klist` shows ticket information

`curl --negotiate -u : -b ~/curl-cookies.txt -c ~/curl-cookies.txt -v http://localhost:8080/actuator`

`kdestroy` destroys ticket


# Kerberos in PostgreSQL

https://paquier.xyz/manuals/postgresql/kerberos/

# Delegation

https://stackoverflow.com/questions/39743700/java-spnego-authentication-kerberos-constrained-delegation-kcd-to-backend-se

https://stackoverflow.com/questions/12529243/delegate-forward-kerberos-tickets-with-spring-security


# Test in docker

```
docker run -it --rm -v /etc/krb5.conf:/etc/krb5.conf --add-host=krb5.local:172.17.0.1 ubuntu
apt update && apt install krb5-user curl iputils-ping iproute2
kinit zdv
klist
```
