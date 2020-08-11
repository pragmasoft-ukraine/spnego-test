package ua.com.pragmasoft.spnego.test.controller;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CredentialsController {

    static final Logger logger = LoggerFactory.getLogger(CredentialsController.class);

    @GetMapping("/credentials")
    public String credentials(Authentication authentication) throws GSSException {

        if (authentication instanceof KerberosServiceRequestToken) {
            KerberosServiceRequestToken token = (KerberosServiceRequestToken) authentication;
        
            if (token.getTicketValidation() == null) {
                logger.debug("No Ticket validation stored");
            } else {
                GSSContext context = token.getTicketValidation().getGssContext();
                var delegated = context.getDelegCred();
                logger.debug("Delegated credentials {}", null != delegated ? delegated.getName() : " are not available");
            }
        }

        return authentication.getName();

    }
    
}