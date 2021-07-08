package com.kingsoft.kubemon.thread;


import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author wangjunjie
 */
@Slf4j
public class ThreadPools {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_SECONDS = 30;
    private static final int QUEUE_SIZE = 10000;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT;

    private static final int IO_MAX = Math.max(2, CPU_COUNT * 2);

    private static final int MIXED_MAX = 128;  //最大线程数
    public static final String MIXED_THREAD_AMOUNT = "mixed.thread.amount";

    public static ThreadPoolExecutor getCpuIntenseThreadPool(String prefix) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                MAXIMUM_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new CustomThreadFactory(prefix, "cpu"));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        Runtime.getRuntime().addShutdownHook(
                new ShutdownHookThread("CPUIntenseThreadPool",
                        () -> shutdownThreadPoolGracefully(threadPoolExecutor)));
        return threadPoolExecutor;
    }

    public static ThreadPoolExecutor getIoIntenseThreadPool(String prefix) {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                IO_MAX,
                IO_MAX,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new CustomThreadFactory(prefix, "io"));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        Runtime.getRuntime().addShutdownHook(
                ShutdownHookThread.of("IOIntenseThreadPool",
                        shutdownThreadPoolGracefully(threadPoolExecutor)));
        return threadPoolExecutor;
    }

    public static ThreadPoolExecutor getMixedThreadPool(String prefix) {
        //如果没有对 mixed.thread.amount 做配置，则使用常量 MIXED_MAX 作为线程数
        final int max = null != System.getProperty(MIXED_THREAD_AMOUNT)
                ? Integer.parseInt(System.getProperty(MIXED_THREAD_AMOUNT))
                : MIXED_MAX;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                max,
                max,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_SIZE),
                new CustomThreadFactory(prefix, "mixed"));
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        Runtime.getRuntime().addShutdownHook(
                ShutdownHookThread.of("MixedThreadPool",
                        shutdownThreadPoolGracefully(threadPoolExecutor)));
        return threadPoolExecutor;
    }

    public static ScheduledThreadPoolExecutor getSeqOrScheduledExecutor(String prefix) {
        final ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(
                1,
                new CustomThreadFactory(prefix, "seq"));
        Runtime.getRuntime().addShutdownHook(
                ShutdownHookThread.of("SeqOrScheduledThreadPool",
                        shutdownThreadPoolGracefully(threadPoolExecutor)));
        return threadPoolExecutor;
    }

    public static void executeOrdered(ScheduledThreadPoolExecutor executor, Runnable command) {
        executor.execute(command);
    }

    public static void schedule(ScheduledThreadPoolExecutor executor,
                                Runnable command, int i, TimeUnit unit) {
        executor.schedule(command, i, unit);
    }

    public static void scheduleAtFixedRate(ScheduledThreadPoolExecutor executor,
                                           Runnable command, int i, TimeUnit unit) {
        executor.scheduleAtFixedRate(command, i, i, unit);
    }

    public static void sleepSeconds(int second) {
        sleepMilliSeconds(second * 1000L);
    }

    public static void sleepMilliSeconds(long millisecond) {
        /*
         * 1. LockSupport.park() 的实现原理是通过二元信号量做的阻塞
         * 要注意的是，这个信号量最多只能加到 1. unpark()方法会释放一个许可证 park()方法则是获取许可证
         * 如果当前没有许可证，则进入休眠状态直到许可证被释放了才被唤醒 无论执行多少次 unpark()方法也最多只会有一个许可证
         *
         * 2. wait、notify 必须能保证 wait 先于 notify 方法执行
         * 如果 notify 方法比 wait 方法晚执行会导致因 wait方法进入休眠的线程接收不到唤醒通知
         *
         * 3. park方法不会抛出 InterruptedException 但是也会响应中断
         */
        LockSupport.parkNanos(millisecond * 1000L * 1000L);
    }

    public static String getCurrentThreadName() {
        return getCurrentThread().getName();
    }

    public static long getCurrentThreadId() {
        return getCurrentThread().getId();
    }

    public static Thread getCurrentThread() {
        return Thread.currentThread();
    }

    /**
     * 调用栈中的类名
     */
    public static String stackClassName(int level) {
        // Thread.currentThread().getStackTrace()[1]是当前方法 curClassName 执行堆栈
        // Thread.currentThread().getStackTrace()[2]就是 curClassName 的 上一级的方法堆栈 以此类推
        return Thread.currentThread().getStackTrace()[level].getClassName();//调用的类名
    }

    /**
     * 调用栈中的方法名称
     */
    public static String stackMethodName(int level) {
        // Thread.currentThread().getStackTrace()[1]是当前方法 curMethodName 执行堆栈
        // Thread.currentThread().getStackTrace()[2]就是 curMethodName 的 上一级的方法堆栈 以此类推
        return Thread.currentThread().getStackTrace()[level].getMethodName();//调用的类名
    }


    private static class CustomThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String threadTag;

        CustomThreadFactory(String prefix, String threadTag) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null)
                    ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
            this.threadTag = String.format("%s-%s-%s-",
                    prefix, poolNumber.getAndIncrement(), threadTag);
        }

        @Override
        public Thread newThread(Runnable target) {
            /*
             * 关于 stackSize
             * The requested stack size for this thread, or 0 if the creator did
             * not specify a stack size.  It is up to the VM to do whatever it
             * likes with this number; some VMs will ignore it.
             */
            Thread t = new Thread(
                    group,
                    target,
                    threadTag + threadNumber.getAndIncrement(),
                    0);
            /*
             * Thread.init 中 daemon 和 priority 是从 currentThread 继承而来
             * 因此需要重置为 非 daemon 和 normal 优先级
             */
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }


    public static Runnable shutdownThreadPoolGracefully(ExecutorService threadPool) {
        if (threadPool == null || threadPool.isTerminated()) {
            return () -> {
            };
        }
        try {
            threadPool.shutdown();   //拒绝接受新任务
        } catch (SecurityException | NullPointerException e) {
            return () -> {
            };
        }
        return () -> {
            try {
                // 等待 60 s，等待线程池中的任务完成执行
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    // 调用 shutdownNow 取消正在执行的任务
                    threadPool.shutdownNow();
                    // 再次等待 60 s，如果还未结束，可以再次尝试，或则直接放弃
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("Tasks in thread pool are not finished properly.");
                    }
                }
            } catch (InterruptedException ie) {
                // 捕获异常，重新调用 shutdownNow
                threadPool.shutdownNow();
            }
            //任然没有关闭，循环关闭1000次，每次等待10毫秒
            if (!threadPool.isTerminated()) {
                try {
                    for (int i = 0; i < 1000; i++) {
                        if (threadPool.awaitTermination(10, TimeUnit.MILLISECONDS)) {
                            break;
                        }
                        threadPool.shutdownNow();
                    }
                } catch (Throwable e) {
                    System.err.println(e.getMessage());
                }
            }
        };
    }


    public static class ShutdownHookThread extends Thread {

        private final Runnable runnable;

        private volatile boolean alreadyShutdown = false;

        /**
         * Create the standard hook thread, with a call back, by using {@link Callable} interface.
         *
         * @param name     thread name
         * @param runnable The call back function.
         */
        private ShutdownHookThread(String name, Runnable runnable) {
            super("JVM exit hook(" + name + ")");
            this.runnable = runnable;
        }

        public static ShutdownHookThread of(String name, Runnable runnable) {
            return new ShutdownHookThread(name, runnable);
        }

        @Override
        public void run() {
            synchronized (this) {
                log.info("{} starting.... ", getName());
                if (!this.alreadyShutdown) {
                    this.alreadyShutdown = true;
                    long beginTime = System.currentTimeMillis();
                    try {
                        this.runnable.run();
                    } catch (Exception e) {
                        log.error(getName() + " error: " + e.getMessage());
                    }
                    long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                    log.info(getName() + "  used(ms): " + consumingTimeTotal);
                }
            }
        }
    }
}
