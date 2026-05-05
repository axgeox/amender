package ax.amender.uix;

import ax.amender.util.SyntaxAnalysisUtils;
import ax.amender.util.SyntaxAnalysisUtils.IssueType;
import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import org.languagetool.rules.RuleMatch;

/**
 * Aplica y limpia los resaltados visuales sobre un componente de texto.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class TextComponentHighlighter {

    private static final Logger LOGGER = Logger.getLogger(TextComponentHighlighter.class.getName());

    private final JTextComponent component;
    private final Map<IssueType, WavyPainter> painters = new EnumMap<>(IssueType.class);
    private final ArrayList<Object> highlightTags = new ArrayList<>(50);

    /**
     * Crea un resaltador ligado a un componente Swing.
     *
     * @param component componente objetivo
     */
    public TextComponentHighlighter(final JTextComponent component) {
        this.component = Objects.requireNonNull(component, "component");

        // Prepara los pintores fijos para cada categoría visual.
        painters.put(IssueType.SPELLING, new WavyPainter(Color.RED));
        painters.put(IssueType.GRAMMAR, new WavyPainter(Color.BLUE.brighter()));
        painters.put(IssueType.STYLE, new WavyPainter(Color.ORANGE));
        painters.put(IssueType.PUNCTUATION, new WavyPainter(Color.MAGENTA));
        painters.put(IssueType.OTHER, new WavyPainter(Color.GREEN.darker()));
    }

    /**
     * Renderiza la lista completa de coincidencias visibles.
     *
     * @param matches coincidencias a pintar
     */
    public void render(final List<RuleMatch> matches) {
        clear();

        if (matches.isEmpty()) {
            return;
        }

        final Highlighter highlighter = component.getHighlighter();

        // OPTIMIZACIÓN: Preasignar memoria exacta para evitar re-dimensionar el array internamente
        highlightTags.ensureCapacity(matches.size());

        for (final RuleMatch match : matches) {
            try {
                final IssueType issueType = SyntaxAnalysisUtils.resolveType(match);
                final WavyPainter painter = painters.get(issueType);
                if (painter != null) {
                    final Object tag = highlighter.addHighlight(match.getFromPos(), match.getToPos(), painter);
                    highlightTags.add(tag);
                }
            } catch (final BadLocationException ex) {
                LOGGER.log(Level.FINE, "No se pudo resaltar una coincidencia.", ex);
            }
        }
    }

    /**
     * Limpia solo los resaltados creados por esta instancia.
     */
    public void clear() {
        if (highlightTags.isEmpty()) {
            return; // Evitar iteraciones inútiles
        }
        final Highlighter highlighter = component.getHighlighter();
        for (int i = 0; i < highlightTags.size(); i++) {
            highlighter.removeHighlight(highlightTags.get(i));
        }
        highlightTags.clear();
    }
}
