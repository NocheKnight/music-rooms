package ru.music.room.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig implements WebMvcConfigurer {

    /**
     * TaskExecutor для @Async методов, использующий виртуальные потоки.
     * Java 25: Thread.ofVirtual() создаёт виртуальные потоки.
     */
    @Bean
    public AsyncTaskExecutor virtualThreadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            // Запускаем каждый таск в новом виртуальном потоке
            return () -> Thread.startVirtualThread(runnable);
        });
        executor.setThreadNamePrefix("virtual-");
        executor.initialize();
        return executor;
    }

    /**
     * Настройка асинхронной поддержки Spring MVC (например, Callable responses).
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(new VirtualThreadTaskExecutor());
    }

    /**
     * Простая реализация TaskExecutor для виртуальных потоков.
     */
    private static class VirtualThreadTaskExecutor implements AsyncTaskExecutor {
        @Override
        public void execute(Runnable task, long startTimeout) {
            Thread.startVirtualThread(task);
        }

        @Override
        public void execute(Runnable task) {
            Thread.startVirtualThread(task);
        }
    }
}