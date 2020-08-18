package ua.com.pragmasoft.security.kerberos;

import java.security.AccessController;
import java.util.Optional;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.springframework.security.access.AccessDeniedException;

public interface KerberosContextUtils {

    static String getSubjectUserName() {
        return Optional.ofNullable(Subject.getSubject(AccessController.getContext()))
                .map(contextSubject -> contextSubject.getPrincipals(KerberosPrincipal.class))
                .flatMap(kerberosPrincipalsSet -> kerberosPrincipalsSet.stream().findAny())
                .map(KerberosPrincipal::getName)
                .orElseThrow(()-> new AccessDeniedException("Cannot obtain thread bound javax.security.auth.Subject, " +
                    "or it does not contain javax.security.auth.kerberos.KerberosPrincipal, " +
                    "possibly method is not annotated with @ua.com.pragmasoft.security.kerberos.WithKerberosAuth annotation"));
    }
   
}