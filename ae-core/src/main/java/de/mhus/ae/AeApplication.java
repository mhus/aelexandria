package de.mhus.ae;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"de.mhus.ae"})
@Slf4j
@Push(PushMode.AUTOMATIC)
public class AeApplication implements AppShellConfigurator  {

    public static void main(String[] args) {
        SpringApplication.run(AeApplication.class, args);
    }

    public static boolean canRestart() {
        return "true".equals(System.getenv("AE_RESTART_POSSIBLE"));
    }

    public static void restart() {
        LOGGER.info("Restarting application");
        System.exit(101); // tell the script to restart the application
    }

    @Bean
    ScheduledExecutorService scheduler() {
        return java.util.concurrent.Executors.newScheduledThreadPool(10);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                LOGGER.debug("- Bean loaded: {}",beanName);
            }
        };
    }
}
