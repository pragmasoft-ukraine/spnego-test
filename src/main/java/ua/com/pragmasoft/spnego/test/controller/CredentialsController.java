package ua.com.pragmasoft.spnego.test.controller;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sun.security.jgss.ExtendedGSSCredential;

@RestController
public class CredentialsController {

    static final Logger logger = LoggerFactory.getLogger(CredentialsController.class);

    @Value("${spring.datasource.url}")
	String jdbcUrl;

    @GetMapping("/credentials")
    public String credentials(Authentication authentication) throws GSSException, PrivilegedActionException {

        if (!(authentication instanceof KerberosServiceRequestToken)) {
            throw new BadCredentialsException("No Ticket validation stored");
        }
        KerberosServiceRequestToken token = (KerberosServiceRequestToken) authentication;
    
        if (token.getTicketValidation() == null) {
            throw new BadCredentialsException("No Ticket validation stored");
        } 

        GSSContext context = token.getTicketValidation().getGssContext();

        GSSCredential delegated = context.getDelegCred();
        
        logger.debug("Delegated credentials {}", null != delegated ? delegated.getName() : " are not available");

        // to use server credentials:
        final var serverSubject = token.getTicketValidation().subject();
        ExtendedGSSCredential serverCred = serverSubject.getPrivateCredentials(ExtendedGSSCredential.class).iterator().next();

        var clientName = context.getSrcName();

        delegated = serverCred.impersonate(clientName);

        // final var user = subject.getPrincipals().iterator().next().getName();

        // to use delegated credentials:
        final var user = clientName.toString(); // token.getTicketValidation().username();
        final var subject = new Subject(true, singleton(new KerberosPrincipal(user)), emptySet(), singleton(delegated));


        logger.debug("Subject {}", subject);

        var currentDbUserName = Subject.doAs(subject, new PrivilegedExceptionAction<String>() {
            public String run() throws Exception {

                final SingleConnectionDataSource ds = new SingleConnectionDataSource(jdbcUrl, user, "", false);
                try {
                    JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
                    return jdbcTemplate.queryForObject("select current_user", String.class);
                } finally {
                    ds.destroy();
                }
            }
        });

        return "Current DB user name: " + currentDbUserName;


    }
    
}