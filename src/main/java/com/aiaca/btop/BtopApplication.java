 package com.aiaca.btop;

 import com.aiaca.btop.security.jwt.JwtProperties;
 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.boot.context.properties.EnableConfigurationProperties;

 @SpringBootApplication
 @EnableConfigurationProperties(JwtProperties.class)
 public class BtopApplication {

 	public static void main(String[] args) {
 		SpringApplication.run(BtopApplication.class, args);
	}
}

/*
package com.aiaca.btop;

import com.aiaca.btop.config.SpringSecurityConfig;
import com.aiaca.btop.security.jwt.JwtProperties;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;


@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        MybatisAutoConfiguration.class
})
@ComponentScan(
        basePackages = "com.aiaca.btop",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "com\\.aiaca\\.btop\\.member\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX,
                        pattern = "com\\.aiaca\\.btop\\.security\\.jwt\\..*"),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SpringSecurityConfig.class)
        }
)
public class BtopApplication {
    public static void main(String[] args) {
        SpringApplication.run(BtopApplication.class, args);
    }
}
*/
