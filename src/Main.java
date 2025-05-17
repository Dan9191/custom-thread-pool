import util.CustomThreadPool;

import java.util.concurrent.*;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        CustomThreadPool pool = new CustomThreadPool(2, 4, 5, 1, 5, TimeUnit.SECONDS);

        // отправка задач на выполнениет
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            pool.execute(() -> {
                LOGGER.info(String.format("Task %d started", taskId));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.info(String.format("Task %d completed", taskId));
            });
        }

        // Имитация действия
        try {
            for (int i = 11; i <= 15; i++) {
                final int taskId = i;
                pool.execute(() -> {
                    LOGGER.info(String.format("Task %d started", taskId));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    LOGGER.info(String.format("Task %d completed", taskId));
                });
            }
        } catch (RejectedExecutionException e) {
            LOGGER.severe("Some tasks were rejected due to pool overload");
        }

        // время на выполнение задач
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Остановка пула
        pool.shutdown();
        LOGGER.info("Pool shutdown initiated");

        // Ожидание завершения задач
        while (!pool.isShutdown()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("All tasks completed, pool terminated");
    }
}