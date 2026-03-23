package br.com.dariokruger.dbmetricsmonitor;

import br.com.dariokruger.dbmetricsmonitor.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class DbMetricsMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbMetricsMonitorApplication.class, args);
    }
}
