package ua.com.pragmasoft.spnego.test.conf;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.springframework.util.Assert;


/**
 * TypedKrb5LoginModuleConfiguration provides typed configuration for a Krb5LoginModule
 */
public class TypedKrb5LoginModuleConfiguration extends Configuration {

    final Map<String,String> options = new HashMap<>(8);

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return new AppConfigurationEntry[] { new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, this.options), };
    }

    public static TypedKrb5LoginModuleConfiguration create() {
        return new TypedKrb5LoginModuleConfiguration();
    }

    public TypedKrb5LoginModuleConfiguration withPrincipal(String principal) {
        return withStringOption("principal", principal);
    }

    public TypedKrb5LoginModuleConfiguration withKeyTab(String keyTab) {
        this.options.put("useKeyTab", "true");
        return withStringOption("keyTab", keyTab);
    }

    public TypedKrb5LoginModuleConfiguration withStoreKey(boolean storeKey) {
        return withBooleanOption("storeKey", storeKey);
    }

    public TypedKrb5LoginModuleConfiguration withDoNotPrompt(boolean doNotPrompt) {
        return withBooleanOption("doNotPrompt", doNotPrompt);
    }

    public TypedKrb5LoginModuleConfiguration withIsInitiator(boolean isInitiator) {
        return withBooleanOption("isInitiator", isInitiator);
    }

    public TypedKrb5LoginModuleConfiguration withDebug() {
        return withBooleanOption("debug", true);
    }

    public TypedKrb5LoginModuleConfiguration withStringOption(String name, String value) {
        Assert.hasText(name, "Please provide option name");
        Assert.hasText(value, "Please provide option value");
        options.put(name, value);
        return this;
    }

    public TypedKrb5LoginModuleConfiguration withBooleanOption(String name, boolean value) {
        Assert.hasText(name, "Please provide option name");
        options.put(name, String.valueOf(value));
        return this;
    }
}