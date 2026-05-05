package ax.amender.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Planifica tareas con retraso y cancela la ejecución previa pendiente.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class DebounceScheduler {

    private static final Logger LOGGER = Logger.getLogger(DebounceScheduler.class.getName());
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(new SchedulerThreadFactory());

    private final long delayMillis;
    private ScheduledFuture<?> pendingTask;

    /**
     * Crea un planificador debounce con un retraso fijo.
     *
     * @param delayMillis retraso en milisegundos
     */
    public DebounceScheduler(final long delayMillis) {
        this.delayMillis = delayMillis;
    }

    /**
     * Agenda una tarea y cancela la pendiente anterior.
     *
     * @param task tarea a ejecutar
     */
    public synchronized void schedule(final Runnable task) {
        // Optimización: Solo cancelamos si hay algo real, evitando llamadas costosas al executor
        if (pendingTask != null && !pendingTask.isDone()) {
            pendingTask.cancel(false);
        }
        pendingTask = EXECUTOR.schedule(()
                -> runSafely(task), delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancela la tarea pendiente, si existe.
     */
    public synchronized void cancel() {
        if (pendingTask != null) {

            // Detiene la tarea pendiente sin interrumpir tareas ya iniciadas.
            pendingTask.cancel(false);
            pendingTask = null;
        }
    }

    /**
     * Ejecuta una tarea controlando cualquier excepción no comprobada.
     *
     * @param task tarea objetivo
     */
    private static void runSafely(final Runnable task) {
        try {

            // Ejecuta la tarea protegida contra fallos de tiempo de ejecución.
            task.run();
        } catch (final RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "Fallo durante la ejecución diferida.", ex);
        }
    }

    /**
     * Fábrica de hilos para el planificador.
     */
    private static final class SchedulerThreadFactory implements ThreadFactory {

        /**
         * Crea un hilo daemon de baja prioridad.
         *
         * @param runnable tarea del hilo
         * @return hilo configurado
         */
        @Override
        public Thread newThread(final Runnable runnable) {
            final Thread thread = new Thread(runnable, "amender-debounce");

            // Ajusta el hilo para trabajo auxiliar de bajo impacto.
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        }
    }
}
