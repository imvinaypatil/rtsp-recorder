package recorder.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread safe.
 *
 *
 */
public abstract class Executor {

    public static interface Listener<E extends Executor> {

        public static enum Event {
            START, STOP, STOPPING, CRASH;
        }

        public default void onStart(E executor) {
        }

        public default void onStop(E executor) {
        }

        public default void onStoping(E executor) {
        }

        public default void onCrash(E executor) {
        }

        public default void onAll(E executor, Event event) {
            switch (event) {
                case START:
                    onStart(executor);
                    break;
                case STOP:
                    onStop(executor);
                    break;
                case STOPPING:
                    onStoping(executor);
                    break;
                case CRASH:
                    onCrash(executor);
            }
        }
    }

    private Thread thread = null;
    private final Set<Listener> listeners = Collections.synchronizedSet(new HashSet<>());
    private Exception lastCrashException = null;
    private volatile boolean stopFlag = false;
    private final Object sleepSync = new Object();
    /**
     * This lock needs for ordering invocations of start() method and others.
     */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public abstract void run() throws Exception;
    public abstract void runPacket() throws Exception;

    public final void start() {
        readWriteLock.writeLock().lock();
        try {
            if (isExecuting()) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Executor started and not stopped yet.");
                throw new RuntimeException("Executor started and not stopped yet.");
            }

            stopFlag = false;
            thread = new Thread(() -> {
                try {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINER, String.format("Executor started. Thread \"%s\".", thread.getName()));
                    runListeners(Listener.Event.START);
                    run();
                    runListeners(Listener.Event.STOP);
                    Logger.getLogger(this.getClass().getName()).log(Level.FINER, "Executor stopped.");
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Executor crashed.", e);
                    lastCrashException = e;
                    runListeners(Listener.Event.CRASH);
                }
            });
            String executorName = getName();
            thread.setName(thread.getName() + " / executor thread" + ((executorName == null) ? "" : " / " + executorName));
            thread.start();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public final void startPacket() {
        readWriteLock.writeLock().lock();
        try {
            if (isExecuting()) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Executor started and not stopped yet.");
                throw new RuntimeException("Executor started and not stopped yet.");
            }

            stopFlag = false;
            thread = new Thread(() -> {
                try {
                    Logger.getLogger(this.getClass().getName()).log(Level.FINER, String.format("Executor started. Thread \"%s\".", thread.getName()));
                    runListeners(Listener.Event.START);
                    runPacket();
                    runListeners(Listener.Event.STOP);
                    Logger.getLogger(this.getClass().getName()).log(Level.FINER, "Executor stopped.");
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Executor crashed.", e);
                    lastCrashException = e;
                    runListeners(Listener.Event.CRASH);
                }
            });
            String executorName = getName();
            thread.setName(thread.getName() + " / executor thread" + ((executorName == null) ? "" : " / " + executorName));
            thread.start();
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void runListeners(Listener.Event event) {
        listeners.forEach((listener) -> {
            try {
                listener.onAll(this, event);
            } catch (Exception e) {
                StringBuilder warningMessageSB = new StringBuilder();
                warningMessageSB
                        .append("Exception in listener.")
                        .append("Event type: ").append(event).append(".");
                if (event == Listener.Event.CRASH) {
                    warningMessageSB.append("lastCrashException: ").append(lastCrashException);
                }
                Logger.getLogger(this.getClass().getName())
                        .log(
                                Level.WARNING,
                                warningMessageSB.toString(),
                                e
                        );
            }
        });
    }

    public final void stop() {
        stop(false);
    }

    public final void stop(boolean interrupt) {
        readWriteLock.readLock().lock();
        try {
            synchronized (sleepSync) {
                stopFlag = true;
                if (interrupt && isExecuting()) {
                    thread.interrupt();
                }
                sleepSync.notify();
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
        runListeners(Listener.Event.STOPPING);
    }

    protected final boolean isStoping() {
        readWriteLock.readLock().lock();
        try {
            return isExecuting() && (thread.isInterrupted() || stopFlag);
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final boolean isExecuting() {
        readWriteLock.readLock().lock();
        try {
            return thread != null && thread.isAlive();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    protected final void sleep(long millis) throws InterruptedException {
        readWriteLock.readLock().lock();
        try {
            if (thread != Thread.currentThread()) {
                return;
            }
            synchronized (sleepSync) {
                if (!isStoping()) {
                    sleepSync.wait(millis);
                }
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final void waitFor() throws InterruptedException {
        readWriteLock.readLock().lock();
        try {
            if (thread != null) {
                thread.join();
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final void stopAndWaitFor() throws InterruptedException {
        readWriteLock.readLock().lock();
        try {
            stop();
            waitFor();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final void waitForInfinitely() {
        readWriteLock.readLock().lock();
        try {
            Logger.getLogger(this.getClass().getName()).log(Level.FINER, "Executor waitForInfinitely().");
            if (thread != null) {
                Executor.waitForInfinitely(thread);
            }
        } finally {
            readWriteLock.readLock().unlock();
        }

    }

    public final void stopAndWaitForInfinitely() {
        readWriteLock.readLock().lock();
        try {
            stop();
            waitForInfinitely();
        } finally {
            readWriteLock.readLock().unlock();
        }

    }

    public final boolean isCrashed() {
        readWriteLock.readLock().lock();
        try {
            return lastCrashException != null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final Exception getLastCrashException() {
        readWriteLock.readLock().lock();
        try {
            return lastCrashException;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final void clearCrash() {
        readWriteLock.readLock().lock();
        try {
            lastCrashException = null;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public final Set<Listener> getListeners() {
        return listeners;
    }

    public String getName() {
        return "";
    }

    public final static void waitForInfinitely(Thread thread) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        // Last 5 elements from stack trace for warning message.
        StringBuilder warningMessageBuilder = new StringBuilder();
        for (int i = stackTraceElements.length; i > 0 && i > stackTraceElements.length - 5; i--) {
            StackTraceElement stackTraceElement = stackTraceElements[i - 1];
            warningMessageBuilder
                    .append(stackTraceElement.getClassName())
                    .append(": ")
                    .append(stackTraceElement.getLineNumber())
                    .append(System.lineSeparator());
        }


        boolean interrupted = false;
        while (thread.isAlive()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Logger.getLogger(Executor.class.getName())
                        .log(
                                Level.WARNING,
                                String.format(
                                        "Can't stop. Waiting infinitely started. Thread \"%s\" waiting for thread \"%s\"",
                                        Thread.currentThread().getName(),
                                        thread.getName()
                                )
                                + System.lineSeparator()
                                + warningMessageBuilder,
                                e
                        );
                interrupted = true;
            }
        }
        Logger.getLogger(Executor.class.getName())
                .log(
                        Level.FINER,
                        String.format(
                                "Waiting infinitely stopped. Thread \"%s\" waited for thread \"%s\"",
                                Thread.currentThread().getName(),
                                thread.getName()
                        )
                );
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

}
