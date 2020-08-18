package ua.com.pragmasoft.spnego.test.conf;

import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import ua.com.pragmasoft.security.kerberos.KerberosTicketValidatorImpl;
import ua.com.pragmasoft.security.kerberos.TypedKrb5LoginModuleConfiguration;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${app.security.kerberos.service-principal:HTTP/localhost}")
	private String servicePrincipal;

	@Value("${app.security.kerberos.keytab-location:http.keytab}")
	private String keytabLocation;

	@Value("${app.security.kerberos.spnego-entrypoint:/spnego.html}")
	private String spnegoEntrypoint;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .exceptionHandling()
                .authenticationEntryPoint(spnegoEntryPoint())
                .and()
            .authorizeRequests()
                .anyRequest()
                    // .permitAll()
                    .authenticated()
                    .and()
            .addFilterBefore(spnegoAuthenticationProcessingFilter(authenticationManagerBean()), BasicAuthenticationFilter.class);
        // @formatter:on
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(kerberosServiceAuthenticationProvider());
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint(spnegoEntrypoint);
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        return filter;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(kerberosTicketValidator());
        provider.setUserDetailsService(dummyUserDetailsService());
        return provider;
    }

    @Bean
    public KerberosTicketValidator kerberosTicketValidator() {
        final var loginModuleConfiguration = TypedKrb5LoginModuleConfiguration.create().withKeyTab(this.keytabLocation)
                .withPrincipal(this.servicePrincipal).withDoNotPrompt(true).withStoreKey(true).withIsInitiator(true)
                .withDebug();

        try {
            return new KerberosTicketValidatorImpl(loginModuleConfiguration);
        } catch (LoginException e) {
            throw new BadCredentialsException("Bad kerberos credentials", e);
        }
    }

    @Bean
    public UserDetailsService dummyUserDetailsService() {
        return (String userName) -> new User(userName, "notUsed", true, true, true, true,
                AuthorityUtils.createAuthorityList("ROLE_USER"));
    }

}
