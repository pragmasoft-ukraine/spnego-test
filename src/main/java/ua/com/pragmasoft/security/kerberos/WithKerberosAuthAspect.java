package ua.com.pragmasoft.security.kerberos;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class WithKerberosAuthAspect {

    static final Logger logger = LoggerFactory.getLogger(WithKerberosAuthAspect.class);

    @Pointcut("@annotation(auth)")
    public void withKerberosAuthPointcut(WithKerberosAuth auth) {
        // pointcut method is intentionally empty
    }

    @Around("ua.com.pragmasoft.security.kerberos.WithKerberosAuthAspect.withKerberosAuthPointcut(auth)")
    public Object establishJaasAuthContext(ProceedingJoinPoint joinPoint, WithKerberosAuth auth) throws Throwable {

        logger.debug("WithKerberosAuthAspect before invocation {}", auth.value());

        Subject subject = null;

        try {
            final Object result = Subject.doAs(subject, wrappedJointPointProceed(joinPoint));
            logger.debug("WithKerberosAuthAspect after successful invocation");
            return result;
        } catch (ThrowableWrapper w) {
            logger.debug("WithKerberosAuthAspect after exception", w.getCause());
            throw w.getCause();
        }

    }

    static PrivilegedAction<Object> wrappedJointPointProceed(ProceedingJoinPoint joinPoint) {
        return () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                throw new ThrowableWrapper(t);
            }
        };
    }

    static final class ThrowableWrapper extends RuntimeException {

        public ThrowableWrapper(Throwable t) {
            super(t);
        }

        private static final long serialVersionUID = 1L;

    }

}