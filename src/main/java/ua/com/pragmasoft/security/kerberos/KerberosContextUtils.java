package ua.com.pragmasoft.security.kerberos;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.security.AccessController;
import java.util.Optional;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import com.sun.security.jgss.ExtendedGSSCredential; // NOSONAR

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.kerberos.authentication.KerberosServiceRequestToken;

public interface KerberosContextUtils {

    static String getSubjectUserName() {
        return Optional.ofNullable(Subject.getSubject(AccessController.getContext()))
                .map(contextSubject -> contextSubject.getPrincipals(KerberosPrincipal.class))
                .flatMap(kerberosPrincipalsSet -> kerberosPrincipalsSet.stream().findAny())
                .map(KerberosPrincipal::getName)
                .orElseThrow(() -> new AccessDeniedException("Cannot obtain thread bound javax.security.auth.Subject, "
                        + "or it does not contain javax.security.auth.kerberos.KerberosPrincipal, "
                        + "possibly method is not annotated with @ua.com.pragmasoft.security.kerberos.WithKerberosAuth annotation"));
    }

    static Subject getSpringSecurityContectSubject(UseSubject clientOrServer) {
        return Optional.ofNullable((KerberosServiceRequestToken) SecurityContextHolder.getContext().getAuthentication())
                .map(KerberosServiceRequestToken::getTicketValidation)
                .map(ticketValidation -> clientOrServer.equals(UseSubject.SERVICE) ? ticketValidation.subject()
                        : impersonate(ticketValidation.subject(), ticketValidation.username()))
                .orElseThrow(() -> new AccessDeniedException(
                        "Cannot obtain javax.security.auth.Subject from Spring security context"));
    }

    static Subject impersonate(Subject serviceSubject, String clientName) {

        try {
            final ExtendedGSSCredential serverCred = Optional
                    .ofNullable(serviceSubject.getPrivateCredentials(ExtendedGSSCredential.class))
                    .flatMap(privateCredentialsSet -> privateCredentialsSet.stream().findAny())
                    .orElseThrow(() -> new IllegalStateException(
                            "No ExtendedGSSCredential in subject " + serviceSubject.toString()));

            final GSSName clientGssName = GSSManager.getInstance().createName(clientName, null);

            final GSSCredential impersonatedCreds = serverCred.impersonate(clientGssName);

            return new Subject(true, singleton(new KerberosPrincipal(clientName)), emptySet(),
                    singleton(impersonatedCreds));

        } catch (GSSException e) {

            throw new AccessDeniedException("Cannot impersonate " + clientName, e);
        }

    }

}