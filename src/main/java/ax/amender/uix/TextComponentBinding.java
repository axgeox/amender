package ax.amender.uix;

import ax.amender.util.DebounceScheduler;
import ax.amender.util.SyntaxAnalysisUtils;
import ax.amender.util.TextCorrectionService;
import ax.amender.util.TextMatchExtractor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.languagetool.rules.RuleMatch;
import ax.amender.util.DocListener;

/**
 * Coordina la interacción Swing de un componente con los servicios lógicos.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class TextComponentBinding {

    private static final Logger LOGGER = Logger.getLogger(TextComponentBinding.class.getName());
    private static final long ANALYSIS_DELAY_MILLIS = 180L;

    private final JTextComponent component;
    private final DebounceScheduler scheduler = new DebounceScheduler(ANALYSIS_DELAY_MILLIS);
    private final TextComponentHighlighter highlighter;
    private final PopupMenuBuilder popupMenuBuilder = new PopupMenuBuilder();
    private final TextMatchExtractor extractor = new TextMatchExtractor();
    private final AtomicLong requestSequence = new AtomicLong();
    private final DocListener typingListener = event -> scheduleAnalysis();
    private final MouseAdapter popupListener = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent event) {
            showPopup(event);
        }

        @Override
        public void mouseReleased(final MouseEvent event) {
            showPopup(event);
        }
    };

    private volatile TextCorrectionService service;
    private volatile String lastText = "";
    private volatile List<RuleMatch> lastMatches = List.of();
    private boolean installed;

    /**
     * Crea una vinculación entre un componente y un servicio lógico.
     *
     * @param component componente de texto
     * @param service servicio lógico activo
     */
    public TextComponentBinding(final JTextComponent component, final TextCorrectionService service) {
        this.component = Objects.requireNonNull(component, "component");
        this.service = Objects.requireNonNull(service, "service");
        this.highlighter = new TextComponentHighlighter(component);
    }

    /**
     * Instala los listeners Swing necesarios una sola vez.
     */
    public synchronized void install() {
        if (installed) {
            reconfigure(service);
            return;
        }

        // Registra la escucha de cambios de texto.
        component.getDocument().addDocumentListener(typingListener);

        // Registra el menú contextual para incidencias detectadas.
        component.addMouseListener(popupListener);
        installed = true;

        // Precalienta el servicio y lanza el primer análisis.
        service.warmUp();
        scheduleAnalysis();
    }

    /**
     * Reconfigura la vinculación con un nuevo servicio lógico.
     *
     * @param service nuevo servicio activo
     */
    public void reconfigure(final TextCorrectionService service) {
        this.service = Objects.requireNonNull(service, "service");

        // Limpia la caché local para forzar un nuevo análisis completo.
        resetState();

        // Asegura que el nuevo servicio quede precalentado antes de usarse.
        this.service.warmUp();
        scheduleAnalysis();
    }

    /**
     * Añade una palabra al diccionario activo y refresca el componente.
     *
     * @param word palabra a ignorar
     */
    public void addToDictionary(final String word) {
        try {

            // Delega la persistencia de la palabra al servicio actual.
            service.addToDictionary(word);

            // Fuerza un nuevo análisis con el diccionario actualizado.
            resetState();
            scheduleAnalysis();
        } catch (final RuntimeException ex) {
            LOGGER.log(Level.SEVERE, "No se pudo añadir la palabra al diccionario.", ex);
        }
    }

    /**
     * Agenda un nuevo análisis debounce del contenido actual.
     */
    private void scheduleAnalysis() {
        final long requestId = requestSequence.incrementAndGet();
        // OPTIMIZACIÓN: Solo pasamos al hilo secundario si la secuencia no ha sido aplastada por teclas rápidas
        scheduler.schedule(() -> {
            if (requestId != requestSequence.get()) {
                return;
            }
            analyze(requestId);
        });
    }

    /**
     * Analiza el texto actual fuera del flujo de eventos de escritura.
     *
     * @param requestId identificador de la petición
     */
    private void analyze(final long requestId) {
        // OPTIMIZACIÓN: Leer el texto de manera rápida. Si hay muchas pulsaciones, la secuencia corta la ejecución antes del análisis costoso.
        final String text = readText();
        if (requestId != requestSequence.get() || text.equals(lastText)) {
            return;
        }

        final List<RuleMatch> matches = service.analyze(text);

        if (requestId == requestSequence.get()) {
            SwingUtilities.invokeLater(() -> applyMatches(requestId, text, matches));
        }
    }

    /**
     * Aplica un resultado de análisis sobre el componente visual.
     *
     * @param requestId identificador de la petición
     * @param text texto analizado
     * @param matches coincidencias detectadas
     */
    private void applyMatches(final long requestId, final String text, final List<RuleMatch> matches) {
        if (requestId != requestSequence.get()) {
            return;
        }

        lastText = text;
        lastMatches = List.copyOf(matches); // Retenemos una vista inmutable segura
        highlighter.render(lastMatches);
    }

    /**
     * Lee el texto del componente de forma segura.
     *
     * @return instantánea textual actual
     */
    private String readText() {
        if (SwingUtilities.isEventDispatchThread()) {
            return component.getText();
        }

        final String[] textRef = new String[1];
        try {
            SwingUtilities.invokeAndWait(() -> textRef[0] = component.getText());
        } catch (InterruptedException | InvocationTargetException ex) {
            LOGGER.log(Level.WARNING, "Error al leer texto.", ex);
            return lastText; // Fallback seguro
        }
        return textRef[0] == null ? "" : textRef[0];
    }

    /**
     * Muestra el menú contextual cuando el usuario hace clic derecho.
     *
     * @param event evento del ratón
     */
    private void showPopup(final MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        final int position = component.viewToModel2D(event.getPoint());

        if (position < 0) {
            return;
        }

        final List<RuleMatch> matches = lastMatches;
        final Optional<RuleMatch> selectedMatch = SyntaxAnalysisUtils.findMatchAt(matches, position);

        if (selectedMatch.isEmpty()) {
            return;
        }

        final RuleMatch match = selectedMatch.get();
        final String text = component.getText();
        final String word = extractor.extract(text, match);

        // Construye el menú contextual con las acciones disponibles.
        final JPopupMenu menu = popupMenuBuilder.build(
                match,
                replacement -> replace(match, replacement),
                () -> addToDictionary(word));

        menu.show(component, event.getX(), event.getY());
    }

    /**
     * Reemplaza el rango marcado por la sugerencia elegida.
     *
     * @param match coincidencia seleccionada
     * @param replacement texto de reemplazo
     */
    private void replace(final RuleMatch match, final String replacement) {
        final Document document = component.getDocument();

        try {

            // Sustituye el texto marcado por la sugerencia elegida.
            document.remove(match.getFromPos(), match.getToPos() - match.getFromPos());
            document.insertString(match.getFromPos(), replacement, null);
        } catch (final BadLocationException ex) {
            LOGGER.log(Level.WARNING, "Fallo al reemplazar.", ex);
        }
    }

    /**
     * Reinicia la caché y limpia el resaltado visible.
     */
    private void resetState() {

        // Invalida la caché textual y las coincidencias previas.
        lastText = "";
        lastMatches = List.of();

        // Limpia el resaltado actual dentro del EDT.
        SwingUtilities.invokeLater(highlighter::clear);
    }
}
