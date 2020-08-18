package ua.com.pragmasoft.spnego.test.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.stereotype.Service;

import ua.com.pragmasoft.security.kerberos.KerberosContextUtils;
import ua.com.pragmasoft.security.kerberos.WithKerberosAuth;

@Service
public class CredentialsService {

    @Value("${spring.datasource.url}")
    String jdbcUrl;

    @WithKerberosAuth
    public String currentDatabaseUser() {

        final String userName = KerberosContextUtils.getSubjectUserName();

        final SingleConnectionDataSource ds = new SingleConnectionDataSource(jdbcUrl, userName, "", false);
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
            return jdbcTemplate.queryForObject("select current_user", String.class);
        } finally {
            ds.destroy();
        }

    }

}