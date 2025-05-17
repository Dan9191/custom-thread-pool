package util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Фабрика для создания потоков, которая присваивает уникальные имена и логирует события их создания и завершения.
 */
public class CustomThreadFactory implements ThreadFactory {
    private final String namePrefix;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private static final Logger LOGGER = Logger.getLogger(CustomThreadFactory.class.getName());

    CustomThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = namePrefix + threadNumber.getAndIncrement();
        LOGGER.info(String.format("[ThreadFactory] Creating new thread: %s", threadName));
        return new Thread(r, threadName);
    }
}
