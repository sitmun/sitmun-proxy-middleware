package org.sitmun.proxy.middleware.config;

import org.sitmun.proxy.middleware.jobs.MBTilesTask;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration extends DefaultBatchConfigurer {

    @Autowired
    private MBTilesTask task;

    @Value("${spring.batch.job.corePoolSize:2}")
    private Integer corePoolSize;

    @Value("${spring.batch.job.maxPoolSize:4}")
    private Integer maxPoolSize;

    @Value("${spring.batch.job.queueCapacity:10}")
    private Integer queueCapacity;

    @Override
    public JobLauncher createJobLauncher() throws Exception {
        SimpleJobLauncher launcher = new SimpleJobLauncher();
        launcher.setJobRepository(getJobRepository());
        launcher.setTaskExecutor(taskExecutor()); // Aquí lo haces asíncrono
        launcher.afterPropertiesSet();
        return launcher;
    }

    //@Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize); // hilos simultáneos
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity); // cola para jobs
        executor.setThreadNamePrefix("batch-job-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Job mbTilesJob(JobBuilderFactory jobBuilderFactory,
                          StepBuilderFactory stepBuilderFactory) {

        Step step = stepBuilderFactory.get("mbtilesStep")
                .tasklet(mbTilesTask())
                .build();

        return jobBuilderFactory.get("mbtilesJob")
                .start(step)
                .build();
    }

    @Bean
    public Tasklet mbTilesTask() {
        return (contribution, chunkContext) -> {
            task.execute(chunkContext.getStepContext());
            return RepeatStatus.FINISHED;
        };
    }
}
