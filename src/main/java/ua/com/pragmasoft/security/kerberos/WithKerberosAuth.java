package ua.com.pragmasoft.security.kerberos;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithKerberosAuth {
    UseSubject value() default UseSubject.SERVICE;
}