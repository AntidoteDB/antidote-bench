package adbm.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * TextAreaAppender for Log4j 2
 */
@Plugin(
        name = "TextPaneAppender",
        category = "Core",
        elementType = "appender",
        printObject = true)
public final class TextPaneAppender extends AbstractAppender
{

    private static List<JTextPane> textPaneList = new ArrayList<>();

    private TextPaneAppender(String name, Filter filter,
                             Layout<? extends Serializable> layout,
                             final boolean ignoreExceptions)
    {
        super(name, filter, layout, ignoreExceptions);
    }

    /**
     * This method is where the appender does the work.
     *
     * @param event Log event with log data
     */
    @Override
    public void append(LogEvent event)
    {
        final String message = new String(getLayout().toByteArray(event));

        // append log text to TextArea
        try {
            if (event.getLevel().equals(Level.TRACE)) {
                appendToPane(message, Color.PINK.darker().darker(), 8);
            }
            else if (event.getLevel().equals(Level.DEBUG)) {
                appendToPane(message, Color.CYAN.darker().darker(), 10);
            }
            else if (event.getLevel().equals(Level.INFO)) {
                appendToPane(message, Color.GREEN.darker().darker(), 12);
            }
            else if (event.getLevel().equals(Level.WARN)) {
                appendToPane(message, Color.ORANGE.darker().darker(), 14);
            }
            else if (event.getLevel().equals(Level.ERROR)) {
                appendToPane(message, Color.RED.darker().darker(), 16);
            }
            else if (event.getLevel().equals(Level.FATAL)) {
                appendToPane(message, Color.RED, 18);
            }
        } catch (Exception ex) {
            // Do not log exceptions that were caused by logging.
            // ex.printStackTrace();
        }
    }

    /**
     * Factory method. Log4j will parse the configuration and call this factory
     * method to construct the appender with
     * the configured attributes.
     *
     * @param name   Name of appender
     * @param layout Log layout of appender
     * @param filter Filter for appender
     * @return The TextAreaAppender
     */
    @PluginFactory
    public static TextPaneAppender createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter)
    {
        if (name == null) {
            LOGGER.error("No name provided for TextPaneAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new TextPaneAppender(name, filter, layout, true);
    }


    /**
     * Set TextArea to append
     *
     * @param textPane TextArea to append
     */
    public static void addTextPane(JTextPane textPane)
    {
        TextPaneAppender.textPaneList.add(textPane);
    }

    private void appendToPane(String msg, Color c, int fontSize)
    {
        SwingUtilities.invokeLater(() -> {
            for (JTextPane tp : textPaneList) {
                StyledDocument doc = tp.getStyledDocument();

                Style style = tp.addStyle("ConsoleStyle", null);
                StyleConstants.setForeground(style, c);
                StyleConstants.setFontSize(style, fontSize);

                try {
                    doc.insertString(doc.getLength(), msg + "\n", style);
                } catch (BadLocationException e) {
                    //e.printStackTrace();
                }
            }
        });
    }
}
