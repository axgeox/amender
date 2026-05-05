package ax.amender.util;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Listener funcional para cambios de escritura en un documento.
 *
 * @version 1
 * @author Alexy Hernandez
 */
@FunctionalInterface
public interface DocListener extends DocumentListener {

    /**
     * Atiende cualquier cambio producido en el documento.
     *
     * @param event evento del documento
     */
    void onTyping(DocumentEvent event);

    @Override
    default void insertUpdate(final DocumentEvent event) {

        // Redirige las inserciones al manejador común.
        onTyping(event);
    }

    @Override
    default void removeUpdate(final DocumentEvent event) {

        // Redirige las eliminaciones al manejador común.
        onTyping(event);
    }

    @Override
    default void changedUpdate(final DocumentEvent event) {

        // Redirige los cambios de atributos al manejador común.
        onTyping(event);
    }
}
