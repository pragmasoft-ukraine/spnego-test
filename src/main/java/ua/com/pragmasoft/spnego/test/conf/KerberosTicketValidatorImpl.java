package ua.com.pragmasoft.spnego.test.conf;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Optional;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.kerberos.authentication.KerberosTicketValidation;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;

/**
 * Ported from org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator
 * 
 * https://stackoverflow.com/questions/12529243/delegate-forward-kerberos-tickets-with-spring-security
 * 
 * @see org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator
 */
public final class KerberosTicketValidatorImpl implements KerberosTicketValidator {

    final Configuration loginModuleConfiguration;
    final String servicePrincipalName;
    final Subject serviceSubject;
    final String loginModuleName;

    public KerberosTicketValidatorImpl(String loginModuleName) throws LoginException {
        this(Configuration.getConfiguration(), loginModuleName);
    }

    public KerberosTicketValidatorImpl(Configuration loginModuleConfiguration) throws LoginException {
        this(loginModuleConfiguration, "");
    }

    public KerberosTicketValidatorImpl(Configuration loginModuleConfiguration, String loginModuleName) throws LoginException {
        this.loginModuleConfiguration = loginModuleConfiguration;
        this.loginModuleName = loginModuleName;
        this.servicePrincipalName = servicePrincipalNameFromConfiguration();
        var subject = new Subject(false, Set.of(new KerberosPrincipal(this.servicePrincipalName)), Set.of(), Set.of());
        final LoginContext lc = new LoginContext("", subject, null, this.loginModuleConfiguration);
        lc.login();
        this.serviceSubject = lc.getSubject();
    }

    private String servicePrincipalNameFromConfiguration() {
        return Optional.ofNullable(this.loginModuleConfiguration.getAppConfigurationEntry(this.loginModuleName))
            .map(entries -> entries.length > 0 ? entries[0] : null)
            .map(AppConfigurationEntry::getOptions)
            .map(options -> (String) options.get("principal"))
            .orElseThrow(() -> new IllegalArgumentException("'principal' kerberos login module configuration option must be present"));
    }

    @Override
    public KerberosTicketValidation validateTicket(byte[] kerberosTicket) {

        final PrivilegedExceptionAction<KerberosTicketValidation> validateTicketAction = () -> {
            byte[] token = tweakJdkRegression(kerberosTicket);
            GSSName gssName = null;
            GSSContext context = GSSManager.getInstance().createContext((GSSCredential) null);
            while (!context.isEstablished()) {
                token = context.acceptSecContext(token, 0, token.length);
                gssName = context.getSrcName();
                if (gssName == null) {
                    throw new BadCredentialsException("GSSContext name of the context initiator is null");
                }
            }
            return new KerberosTicketValidation(gssName.toString(), servicePrincipalName, token, context);
    
        };

        try {
            return Subject.doAs(this.serviceSubject, validateTicketAction);
        }
        catch (PrivilegedActionException e) {
            throw new BadCredentialsException("Kerberos validation not successful", e);
        }    
    }

    private static byte[] tweakJdkRegression(byte[] token) {

                if (token == null || token.length < 48) {
                    return token;
                }
        
                int[] toCheck = new int[] { 0x06, 0x09, 0x2A, 0x86, 0x48, 0x82, 0xF7, 0x12, 0x01, 0x02, 0x02, 0x06, 0x09, 0x2A,
                        0x86, 0x48, 0x86, 0xF7, 0x12, 0x01, 0x02, 0x02 };
        
                for (int i = 0; i < 22; i++) {
                    if ((byte) toCheck[i] != token[i + 24]) {
                        return token;
                    }
                }
        
                byte[] nt = new byte[token.length];
                System.arraycopy(token, 0, nt, 0, 24);
                System.arraycopy(token, 35, nt, 24, 11);
                System.arraycopy(token, 24, nt, 35, 11);
                System.arraycopy(token, 46, nt, 46, token.length - 24 - 11 - 11);
                return nt;
            }

}