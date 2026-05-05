package ax.amender.uix;

import ax.amender.util.SyntaxAnalysisUtils;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.languagetool.rules.RuleMatch;

/**
 * Construye el menú contextual de sugerencias para una coincidencia.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class PopupMenuBuilder {

    private static final int MAX_SUGGESTIONS = 5;

    /**
     * Crea un menú contextual para una coincidencia concreta.
     *
     * @param match coincidencia seleccionada
     * @param replaceAction acción de reemplazo
     * @param addToDictionaryAction acción para ignorar la palabra
     * @return menú contextual listo para mostrar
     */
    public JPopupMenu build(
            final RuleMatch match,
            final Consumer<String> replaceAction,
            final Runnable addToDictionaryAction) {

        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(replaceAction, "replaceAction");
        Objects.requireNonNull(addToDictionaryAction, "addToDictionaryAction");

        final JPopupMenu menu = new JPopupMenu();
        final List<String> suggestions = SyntaxAnalysisUtils.firstSuggestions(match, MAX_SUGGESTIONS);

        suggestions.forEach(suggestion -> {
            final JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(event -> replaceAction.accept(suggestion));
            menu.add(item);
        });

        if (!suggestions.isEmpty()) {
            menu.addSeparator();
        }

        final JMenuItem addItem = new JMenuItem("Añadir al diccionario");
        addItem.addActionListener(event -> addToDictionaryAction.run());
        menu.add(addItem);

        return menu;
    }
}
