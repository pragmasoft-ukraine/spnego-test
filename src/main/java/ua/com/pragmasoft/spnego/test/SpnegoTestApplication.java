package ua.com.pragmasoft.spnego.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class SpnegoTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpnegoTestApplication.class, args);
	}

}
