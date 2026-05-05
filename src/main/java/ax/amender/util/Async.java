package ax.amender.util;

import java.util.Objects;
import javax.swing.SwingWorker;

/**
 * Representa una tarea que debe ejecutarse en segundo plano.
 *
 * @version 1
 * @author Alexy Hernandez
 */
@FunctionalInterface
public interface Async extends Runnable {

    /**
     * Lanza una tarea funcional en segundo plano.
     *
     * @param task tarea a ejecutar
     */
    static void work(final Runnable task) {
        // Optimización: Usar SwingUtilities para tareas cortas o 
        // un ExecutorService global para evitar crear un Thread/Worker por cada llamada.
        Objects.requireNonNull(task, "task");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                task.run();
                return null;
            }
        }.execute();
    }
}
