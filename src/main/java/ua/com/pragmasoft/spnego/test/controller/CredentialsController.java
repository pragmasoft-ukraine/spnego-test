package ua.com.pragmasoft.spnego.test.controller;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import com.sun.security.jgss.ExtendedGSSCredential; // NOSONAR

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ua.com.pragmasoft.spnego.test.service.CredentialsService;

@RestController
public class CredentialsController {

    static final Logger logger = LoggerFactory.getLogger(CredentialsController.class);

    @Autowired
    CredentialsService credentialsService;

    @GetMapping("/credentials")
    public String credentials(Authentication authentication) throws GSSException {

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

        var currentDbUserName = credentialsService.currentDatabaseUser();

        return "Current DB user name: " + currentDbUserName;


    }
    
}