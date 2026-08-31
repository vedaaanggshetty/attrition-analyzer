package com.example.AuthService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

// TEMPORARY: DataSourceAutoConfiguration and HibernateJpaAutoConfiguration are
// excluded because MySQL/Authentication DB setup has been intentionally
// postponed. This lets the service start without a live database connection.
// REMOVE this exclusion once the real Credential entity/repository (Phase 3+)
// are implemented and MySQL is available.
@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class
})
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
