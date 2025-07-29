package com.colombia.eps.session;

import com.colombia.eps.session.configurations.SessionConfigurations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

import java.util.Date;
import java.util.TimeZone;

@SpringBootApplication
@Import(SessionConfigurations.class)
@Slf4j
public class SessionApplication {
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
		log.debug("Default TimeZone: {}" , TimeZone.getDefault().getID());
		log.debug("Current time in default zone: {}", new Date());
		new SpringApplicationBuilder(SessionApplication.class)
				.web(WebApplicationType.NONE)
				.run(args);
	}
}
