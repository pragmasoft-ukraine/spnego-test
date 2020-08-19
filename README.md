# Kerberos credentials

`kadmin:  ank +ok_to_delegate zdv`

`kadmin:  ank zdv/admin`

`kadmin:  ank -randkey +ok_to_auth_as_delegate HTTP/krb5.local`

`kadmin:  ktadd -k http.keytab HTTP/krb5.local`

`kadmin:  q`

`sudo -iu postgres`

Then as a postgres user:

`kadmin -p zdv/admin`

`kadmin:  ank -randkey postgres/krb5.local`

`kadmin:  ktadd -k /etc/postgresql/12/main/postgresql.keytab postgres/krb5.local`

`kdestroy` clean kerberos ticket cache

`kinit -kt http.keytab HTTP/krb5.local` obtains kerberos ticket for http service, which acts as a delegate

`kvno -k http.keytab -U zdv -P HTTP/krb5.local postgres/krb5.local` obtains s4u2proxy ticket for HTTP service to access postgresql service as user zdv

`klist` shows ticket information, must contain three tickets:

```
Default principal: HTTP/krb5.local@krb5.local

Valid starting     Expires            Service principal
19.08.20 13:38:28  20.08.20 13:38:28  krbtgt/krb5.local@krb5.local
19.08.20 13:38:48  20.08.20 13:38:28  HTTP/krb5.local@krb5.local
	for client zdv@krb5.local
19.08.20 13:38:48  20.08.20 13:38:28  postgres/krb5.local@krb5.local
	for client zdv@krb5.local

```

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

# Set up OpenLDAP as a KDC backend database

https://ubuntu.com/server/docs/service-ldap
https://ubuntu.com/server/docs/service-kerberos-with-openldap-backend


```
ldapadd -x -D cn=admin,dc=krb5,dc=local -W <<EOF
dn: uid=kdc-service,dc=krb5,dc=local
uid: kdc-service
objectClass: account
objectClass: simpleSecurityObject
userPassword: {CRYPT}x
description: Account used for the Kerberos KDC

dn: uid=kadmin-service,dc=krb5,dc=local
uid: kadmin-service
objectClass: account
objectClass: simpleSecurityObject
userPassword: {CRYPT}x
description: Account used for the Kerberos Admin server
EOF

ldappasswd -x -D cn=admin,dc=krb5,dc=local -W -S uid=kdc-service,dc=krb5,dc=local


ldappasswd -x -D cn=admin,dc=krb5,dc=local -W -S uid=kadmin-service,dc=krb5,dc=local

sudo ldapmodify -Q -Y EXTERNAL -H ldapi:/// <<EOF
dn: olcDatabase={1}mdb,cn=config
add: olcAccess
olcAccess: {2}to attrs=krbPrincipalKey
  by anonymous auth
  by dn.exact="uid=kdc-service,dc=krb5,dc=local" read
  by dn.exact="uid=kadmin-service,dc=krb5,dc=local" write
  by self write
  by * none
-
add: olcAccess
olcAccess: {3}to dn.subtree="cn=krbContainer,dc=krb5,dc=local"
  by dn.exact="uid=kdc-service,dc=krb5,dc=local" read
  by dn.exact="uid=kadmin-service,dc=krb5,dc=local" write
  by * none
EOF

sudo kdb5_ldap_util -D cn=admin,dc=krb5,dc=local create -subtrees dc=krb5,dc=local -r krb5.local -s -H ldapi:///

sudo kdb5_ldap_util -D cn=admin,dc=krb5,dc=local stashsrvpw -f /etc/krb5kdc/service.keyfile uid=kdc-service,dc=krb5,dc=local

sudo kdb5_ldap_util -D cn=admin,dc=krb5,dc=local stashsrvpw -f /etc/krb5kdc/service.keyfile uid=kadmin-service,dc=krb5,dc=local

ldapadd -x -D cn=admin,dc=krb5,dc=local -W <<EOF
dn: ou=People,dc=krb5,dc=local
objectClass: organizationalUnit
ou: People
EOF

sudo kadmin.local
kadmin: addprinc +ok_as_delegate zdv
kadmin: addprinc -randkey +ok_to_auth_as_delegate HTTP/krb5.local
kadmin: addprinc -randkey postgres/krb5.local

# Add constrained delegation ACL

ldapmodify -x -D cn=admin,dc=krb5,dc=local -W <<EOF
dn: krbPrincipalName=HTTP/krb5.local@krb5.local,cn=krb5.local,cn=krbContainer,dc=krb5,dc=local
changetype: modify
add: krbAllowedToDelegateTo
krbAllowedToDelegateTo: postgres/krb5.local
EOF
```

#How to set up delegation ACLs

http://kerberos.996246.n3.nabble.com/Constrained-Delegation-error-quot-KDC-policy-rejects-request-quot-td48955.html