package com.github.kagkarlsson.scheduler.boot.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.boot.actuator.DbSchedulerHealthIndicator;
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerStarter;
import com.github.kagkarlsson.scheduler.boot.config.startup.AbstractSchedulerStarter;
import com.github.kagkarlsson.scheduler.boot.config.startup.ContextReadyStart;
import com.github.kagkarlsson.scheduler.boot.config.startup.ImmediateStart;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.google.common.collect.ImmutableList;

import java.util.Objects;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


public class DbSchedulerAutoConfigurationTest {
    private static final Logger log = LoggerFactory.getLogger(DbSchedulerAutoConfigurationTest.class);
    private final ApplicationContextRunner ctxRunner;

    public DbSchedulerAutoConfigurationTest() {
        ctxRunner = new ApplicationContextRunner()
            .withPropertyValues(
                "spring.application.name=db-scheduler-boot-starter-test",
                "spring.profiles.active=integration-test"
            ).withConfiguration(AutoConfigurations.of(
                DataSourceAutoConfiguration.class, DbSchedulerAutoConfiguration.class
            ));
    }

    @Test
    public void it_should_initialize_an_empty_scheduler() {
        ctxRunner.run((AssertableApplicationContext ctx) -> {
            assertThat(ctx).hasSingleBean(DataSource.class);
            assertThat(ctx).hasSingleBean(Scheduler.class);

            ctx.getBean(Scheduler.class).getScheduledExecutions(execution -> {
                fail("No scheduled executions should be present", execution);
            });
        });
    }

    @Test
    public void it_should_initialize_a_scheduler_with_a_single_task() {
        ctxRunner
            .withUserConfiguration(SingleTaskConfiguration.class)
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(Scheduler.class);
                assertThat(ctx).hasSingleBean(Task.class);
                assertThat(ctx).getBean("singleStringTask", Task.class).isNotNull();
            });
    }

    @Test
    public void it_should_initialize_a_scheduler_with_a_multiple_tasks() {
        ctxRunner
            .withUserConfiguration(MultipleTasksConfiguration.class)
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(Scheduler.class);

                ImmutableList.of("firstTask", "secondTask", "thirdTask").forEach(beanName -> {
                    assertThat(ctx).getBean(beanName, Task.class).isNotNull();
                });
            });
    }

    @Test
    public void it_should_autoconfigure_a_health_check() {
        ctxRunner
            .withConfiguration(AutoConfigurations.of(HealthIndicatorAutoConfiguration.class))
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(DbSchedulerHealthIndicator.class);
            });
    }

    @Test
    public void it_should_skip_autoconfiguration_if_explicitly_disabled() {
        ctxRunner
            .withPropertyValues("db-scheduler.enabled=false")
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).doesNotHaveBean(Scheduler.class);
            });
    }

    @Test
    public void it_should_start_as_soon_as_possible() {
        ctxRunner
            .withPropertyValues("db-scheduler.delay-startup-until-context-ready=false")
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(Scheduler.class);

                assertThat(ctx).hasSingleBean(DbSchedulerStarter.class);
                assertThat(ctx).hasSingleBean(ImmediateStart.class);
                assertThat(ctx).doesNotHaveBean(ContextReadyStart.class);

            });
    }

    @Test
    public void it_should_start_when_the_context_is_ready() {
        ctxRunner
            .withPropertyValues("db-scheduler.delay-startup-until-context-ready=true")
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(Scheduler.class);

                assertThat(ctx).hasSingleBean(DbSchedulerStarter.class);
                assertThat(ctx).hasSingleBean(ContextReadyStart.class);
                assertThat(ctx).doesNotHaveBean(ImmediateStart.class);

            });
    }

    @Test
    public void it_should_support_custom_starting_strategies() {
        ctxRunner
            .withUserConfiguration(CustomStarterConfiguration.class)
            .run((AssertableApplicationContext ctx) -> {
                assertThat(ctx).hasSingleBean(Scheduler.class);

                assertThat(ctx).hasSingleBean(DbSchedulerStarter.class);
                assertThat(ctx).doesNotHaveBean(ContextReadyStart.class);
                assertThat(ctx).doesNotHaveBean(ImmediateStart.class);
            });
    }

    @Configuration
    static class SingleTaskConfiguration {
        @Bean
        Task<String> singleStringTask() {
            return namedStringTask("single-string-task");
        }
    }

    @Configuration
    static class MultipleTasksConfiguration {
        @Bean
        Task<String> firstTask() {
            return namedStringTask("first-task");
        }

        @Bean
        Task<String> secondTask() {
            return namedStringTask("second-task");
        }

        @Bean
        Task<String> thirdTask() {
            return namedStringTask("third-task");
        }
    }

    @Configuration
    static class CustomStarterConfiguration extends SingleTaskConfiguration {
        @Bean
        DbSchedulerStarter someCustomStarter(Scheduler scheduler) {
            return new SomeCustomStarter(scheduler);
        }

        static class SomeCustomStarter extends AbstractSchedulerStarter {
            SomeCustomStarter(Scheduler scheduler) {
                super(scheduler);

                try {
                    log.info("Thinking 5 seconds before starting the scheduler");
                    Thread.sleep(5_000);
                    doStart();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static Task<String> namedStringTask(String name) {
        Objects.requireNonNull(name);

        return Tasks
            .oneTime(name, String.class)
            .execute((instance, context) -> {
                log.info("Executed task: {}, ctx: {}", instance, context);
            });
    }
}
