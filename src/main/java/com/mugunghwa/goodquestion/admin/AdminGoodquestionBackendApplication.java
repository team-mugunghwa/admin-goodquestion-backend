package com.mugunghwa.goodquestion.admin;

import com.mugunghwa.goodquestion.admin.global.config.RequiredEnvironmentListener;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AdminGoodquestionBackendApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication application =
                new SpringApplication(AdminGoodquestionBackendApplication.class);
        // 환경변수가 빠졌을 때 원인을 가리는 예외 대신 읽을 수 있는 안내를 내보낸다.
        // 여기서 붙이면 실제로 앱을 띄울 때만 돌고 테스트에는 끼어들지 않는다.
        application.addListeners(new RequiredEnvironmentListener());
        application.run(args);
    }

}
