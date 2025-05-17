package util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class CustomThreadPool implements CustomExecutor {
    private static final Logger LOGGER = Logger.getLogger(CustomThreadPool.class.getName());

    /**
     * Минимальное (базовое) количество потоков.
     */
    private final int corePoolSize;

    /**
     * Максимальное количество потоков.
     */
    private final int maxPoolSize;

    /**
     * Минимальное число «резервных» потоков.
     */
    private final int minSpareThreads;

    /**
     * Время, в течение которого поток может простаивать до завершения
     */
    private final long keepAliveTime;

    /**
     * Единицы измерения времени
     */
    private final TimeUnit timeUnit;

    /**
     * Список очереди задач.
     */
    private final List<BlockingQueue<Runnable>> workQueues;

    /**
     * Список воркеров.
     */
    private final List<Worker> workers;

    /**
     * Фабрика потоков.
     */
    private final CustomThreadFactory threadFactory;

    /**
     * Механизм отказа.
     */
    private final CustomRejectionHandler rejectionHandler;

    /**
     *
     */
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private final AtomicInteger currentQueueIndex = new AtomicInteger(0);

    public CustomThreadPool(int corePoolSize, int maxPoolSize, int queueSize, int minSpareThreads,
                            long keepAliveTime, TimeUnit timeUnit) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.minSpareThreads = minSpareThreads;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.workQueues = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.threadFactory = new CustomThreadFactory("MyPool-worker-");
        this.rejectionHandler = new CustomRejectionHandler();

        for (int i = 0; i < corePoolSize; i++) {
            workQueues.add(new ArrayBlockingQueue<>(queueSize));
        }

        for (int i = 0; i < corePoolSize; i++) {
            Worker worker = new Worker(workQueues.get(i));
            workers.add(worker);
            threadFactory.newThread(worker).start();
        }
    }

    /**
     * Запуск выполнение задач.ь
     *
     * @param command задача
     */
    @Override
    public void execute(Runnable command) {
        if (isShutdown.get()) {
            throw new RejectedExecutionException("Thread pool is shut down");
        }

        // балансировка задач
        int queueIndex = currentQueueIndex.getAndIncrement() % workQueues.size();
        BlockingQueue<Runnable> queue = workQueues.get(queueIndex);

        LOGGER.info(String.format("[Pool] Task accepted into queue #%d: %s", queueIndex, command));

        if (!queue.offer(command)) {
            rejectionHandler.rejectedExecution(command);
        }

        //  поддержание минимально доступных потоков
        if (getIdleThreads() < minSpareThreads && workers.size() < maxPoolSize) {
            Worker worker = new Worker(queue);
            workers.add(worker);
            threadFactory.newThread(worker).start();
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> futureTask = new FutureTask<>(callable);
        execute(futureTask);
        return futureTask;
    }

    @Override
    public void shutdown() {
        isShutdown.set(true);
        for (Worker worker : workers) {
            worker.stop();
        }
    }

    @Override
    public void shutdownNow() {
        isShutdown.set(true);
        for (Worker worker : workers) {
            worker.interrupt();
        }
        for (BlockingQueue<Runnable> queue : workQueues) {
            queue.clear();
        }
    }

    public boolean isShutdown() {
        return isShutdown.get();
    }

    private int getIdleThreads() {
        int idleCount = 0;
        for (Worker worker : workers) {
            if (!worker.isProcessing()) {
                idleCount++;
            }
        }
        return idleCount;
    }

    private class Worker implements Runnable {
        private final BlockingQueue<Runnable> queue;
        private final AtomicBoolean isRunning = new AtomicBoolean(true);
        private volatile boolean isProcessing = false;
        private Thread thread;

        Worker(BlockingQueue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            thread = Thread.currentThread();
            while (isRunning.get() && !isShutdown.get()) {
                try {
                    Runnable task = queue.poll(keepAliveTime, timeUnit);
                    if (task == null) {
                        LOGGER.info(String.format("[Worker] %s idle timeout, stopping", thread.getName()));
                        if (workers.size() > corePoolSize) {
                            stop();
                            workers.remove(this);
                            break;
                        }
                        continue;
                    }

                    isProcessing = true;
                    LOGGER.info(String.format("[Worker] %s executes %s", thread.getName(), task));
                    task.run();
                } catch (InterruptedException e) {
                    LOGGER.info(String.format("[Worker] %s interrupted", thread.getName()));
                    break;
                } finally {
                    isProcessing = false;
                }
            }
            LOGGER.info(String.format("[Worker] %s terminated", thread.getName()));
        }

        boolean isProcessing() {
            return isProcessing;
        }

        void stop() {
            isRunning.set(false);
        }

        void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
        }
    }

    private static class CustomRejectionHandler {
        private static final Logger LOGGER = Logger.getLogger(CustomRejectionHandler.class.getName());

        public void rejectedExecution(Runnable r) {
            LOGGER.severe(String.format("[Rejected] Task %s was rejected due to overload!", r));
            throw new RejectedExecutionException("Task " + r + " rejected due to pool overload");
        }
    }
}