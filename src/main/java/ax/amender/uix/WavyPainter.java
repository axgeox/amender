package ax.amender.uix;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;

/**
 * Dibuja un subrayado ondulado para resaltar incidencias sobre texto Swing.
 *
 * @version 1
 * @author Alexy Hernandez
 */
public final class WavyPainter implements Highlighter.HighlightPainter {

    private static final Logger LOGGER = Logger.getLogger(WavyPainter.class.getName());

    private final Color color;

    /**
     * Crea un pintor ondulado con el color indicado.
     *
     * @param color color de la onda
     */
    public WavyPainter(final Color color) {
        this.color = Objects.requireNonNull(color, "color");
    }

    /**
     * Pinta el subrayado ondulado del rango solicitado.
     *
     * @param graphics contexto gráfico
     * @param start inicio del rango
     * @param end fin del rango
     * @param bounds límites expuestos por Swing
     * @param component componente de texto
     */
    @Override
    public void paint(Graphics graphics, int start, int end, Shape bounds, JTextComponent component) {
        try {
            final Rectangle2D startBounds = component.modelToView2D(start);
            final Rectangle2D endBounds = component.modelToView2D(end);

            if (startBounds == null || endBounds == null) {
                return;
            }

            final Graphics2D g2d = (Graphics2D) graphics.create();
            try {
                g2d.setColor(color);
                g2d.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                drawUnderline(g2d, component, startBounds, endBounds);
            } finally {
                g2d.dispose();
            }
        } catch (final BadLocationException ex) {
            LOGGER.log(Level.FINE, "No se pudo calcular el área del resaltado ondulado.", ex);
        }
    }

    /**
     * Dibuja la onda en una o varias líneas visibles.
     *
     * @param graphics2d contexto 2D
     * @param component componente de texto
     * @param startBounds área inicial
     * @param endBounds área final
     */
    private void drawUnderline(final Graphics2D g2d, final JTextComponent comp, final Rectangle2D startB, final Rectangle2D endB) {
        final Rectangle visible = comp.getVisibleRect();
        final int lineHeight = Math.max(1, (int) Math.round(startB.getHeight()));
        final int firstY = (int) Math.round(startB.getY());
        final int lastY = (int) Math.round(endB.getY());
        final int leftPad = visible.x + 2;
        final int rightPad = visible.x + visible.width - 5;

        if (firstY == lastY) {
            drawWave(g2d,
                    (int) Math.round(startB.getX()),
                    (int) Math.round(endB.getX()),
                    firstY + lineHeight - 2);
            return;
        }

        drawWave(g2d,
                (int) Math.round(startB.getX()),
                rightPad, firstY + lineHeight - 2);

        for (int y = firstY + lineHeight; y < lastY; y += lineHeight) {
            drawWave(g2d, leftPad, rightPad, y + lineHeight - 2);
        }

        drawWave(g2d,
                leftPad,
                (int) Math.round(endB.getX()),
                lastY + lineHeight - 2);
    }

    /**
     * Dibuja una línea ondulada horizontal entre dos posiciones.
     *
     * @param graphics2d contexto gráfico
     * @param startX coordenada inicial
     * @param endX coordenada final
     * @param baseY línea base
     * @param waveHeight altura de la onda
     * @param segmentLength longitud de cada segmento
     */
    private void drawWave(final Graphics2D g2d, final int startX, final int endX, final int baseY) {
        if (startX >= endX) {
            return;
        }

        final Path2D.Float wavePath = new Path2D.Float();
        wavePath.moveTo(startX, baseY);

        boolean goingUp = true;
        final int waveHeight = 2;
        final int segmentLength = 4;

        for (int x = startX; x < endX; x += segmentLength) {
            int nextX = Math.min(x + segmentLength, endX);
            int nextY = baseY + (goingUp ? -waveHeight : waveHeight);
            wavePath.lineTo(nextX, nextY);
            goingUp = !goingUp;
        }
        g2d.draw(wavePath); // Una sola llamada de renderizado
    }
}
