# Spring SPNEGO authentication prototype with S4U constrained delegation used to connect to PostgreSQL database

## Introduction

This application demonstrates the SPNEGO authentication to the web application, and then uses constrained delegation to connect to the PostgreSQL database. 

It was built and tested on Ubuntu Linux and requires the following infrastructure:

* MIT Kerberos KDC, admin server and client utils
* OpenLDAP directory server, set up as a MIT KDC backend. LDAP backend is *required* for constrained delegation to store ACLs, but can be omitted with unconstrained delegation and simple username/password based authentication.
* PostgreSQL database, configured for kerberos authenticaton.
* DNS server or simply valid domain name records in /etc/hosts
* HTTP client. I tested with Firefox and curl, but other browsers like Chrome are documented to also support SPNEGO auth. You may consult [this page](https://docs.cloudera.com/documentation/enterprise/latest/topics/cdh_sg_browser_access_kerberos_protected_url.html) for instructions, how to enable SPNEGO in the most popular browsers.

## How application works

In order to test application, you need the above mentioned environment configured and kerberos accounts created for user (zdv), application itself (HTTP/krb5.local) and PostgreSQL database (postgresql/krb5.local), as explained later. 

Lets assume, the application listens to http://krb5.local:8080/ 

We need to authenticate user to the kerberos first, so that its ticket has been added to its credentials cache. In the case kerberos single sign on is enabled in PAM, user just needs to login to the system, otherwise `kinit` command can be used to authenticate user in kerberos system. Once authenticated, `klist` can be used to verify user's cached credentials and `kdestroy` to destroy credentials and log out. 

If user is authenticated properly, request like `curl --negotiate -u : -b ~/curl-cookies.txt -c ~/curl-cookies.txt -v http://krb5.local:8080/` should return html page with text like *Hello zdv@krb5.local* 

If something went wrong, for example, if you forgot to login to kerberos with `kinit`, you'll get 401 response with the text *SPNEGO authentication is required* instead. 

Once authenticated, client will use session cookie to authenticate other requests, so even if you log out of kerberos with `kdestroy` you still be logged in the application, until you invalidate cookies or session. 

The GET request to the http://krb5.local:8080/credentials creates a delegated token to connect to the database, establishes JDBC connection using this token for kerberos authentication, executes the following query `select current_user` to prove that credentials are actually delegated and returns result in http response to the caller. 

Internally, delegation is implemented as aspect, public class WithKerberosAuthAspect, which binds JAAS Subject with kerberos credentials to the thread, executing method, annotated with @WithKerberosAuth annotation.

This annotation accepts parameter, UseSubject enumeration, with two values: `SERVICE` and `DELEGATED`, allowing to propagate either credentials of the service (spring application) itself, or client, which uses it.

## Environment configuration

### Configure DNS domain 

Add a line to `/etc/hosts`

`127.0.2.1	krb5.local`

### Install and configure OpenLDAP

https://ubuntu.com/server/docs/service-ldap

### Install kerberos KDC and set up OpenLDAP as a KDC backend 

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
### Install PostgreSQL and configure kerberos authentication

https://paquier.xyz/manuals/postgresql/kerberos/


### Create kerberos accounts for user, this spring application and postgresql database

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

### Verify constrained delegation actually works using just kerberos tools

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
## Build and run

`./mvnw spring-boot:run`


`curl --negotiate -u : -b ~/curl-cookies.txt -c ~/curl-cookies.txt -v http://krb5.local:8080/credentials`


## Here's why do we need to rewrite SunJaasKerberosTicketValidator

https://stackoverflow.com/questions/12529243/delegate-forward-kerberos-tickets-with-spring-security


# Test kerberos auth works also in docker container

```
docker run -it --rm -v /etc/krb5.conf:/etc/krb5.conf --add-host=krb5.local:172.17.0.1 ubuntu
apt update && apt install krb5-user curl iputils-ping iproute2
kinit zdv
klist
```

## How to set up delegation ACLs

http://kerberos.996246.n3.nabble.com/Constrained-Delegation-error-quot-KDC-policy-rejects-request-quot-td48955.html

## TODO

Use `LdapUserDetailsService` instead of `dummyUserDetailsService`

