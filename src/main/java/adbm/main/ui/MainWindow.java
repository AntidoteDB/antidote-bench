package adbm.main.ui;

import adbm.antidote.AntidoteClientWrapper;
import adbm.antidote.ui.AntidoteView;
import adbm.docker.DockerfileBuilder;
import adbm.docker.DockerManager;
import adbm.git.GitManager;
import adbm.git.ui.SelectBranchDialog;
import adbm.settings.MapDBManager;
import adbm.settings.ui.SettingsDialog;
import org.apache.commons.lang.time.StopWatch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import javax.swing.text.*;

public class MainWindow
{

    private JTextArea textAreaConsole;
    private JPanel panel;
    private JButton buttonSettings;
    private JButton buttonStartDocker;
    private JButton buttonStartGit;
    private JButton buttonShowGitSettings;
    private JButton buttonStartAntidote;
    private JButton buttonCreateDockerfile;
    private JButton buttonBuildBenchmarkImages;
    private Document document;
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    public MainWindow()
    {
        JFrame frame = new JFrame("ConsoleLog");

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        this.document = textAreaConsole.getDocument();
        ConsoleOutputStream cos = new ConsoleOutputStream(null, System.out);
        System.setOut(new PrintStream(cos, true));
        ConsoleOutputStream cos2 = new ConsoleOutputStream(Color.RED, System.err);
        System.setErr(new PrintStream(cos2, true));
        buttonSettings.addActionListener(e -> {
            if (MapDBManager.isReady())
            new SettingsDialog();
        });
        buttonStartGit.addActionListener(e -> {
            if (!GitManager.isReadyNoText())
            executorService.execute(() -> GitManager.startGit());
            else System.out.println("Git is already started!");
        });
        buttonStartDocker.addActionListener(e -> {
            if (!DockerManager.isReadyNoText())
            executorService.execute(() -> DockerManager.startDocker());
            else System.out.println("Docker is already started!");
        });
        buttonShowGitSettings.addActionListener(e -> {
            if (GitManager.isReady())
            new SelectBranchDialog();
        });
        buttonStartAntidote.addActionListener(e -> {
            if (DockerManager.isReady())
                new AntidoteView(new AntidoteClientWrapper("TestAntidote"));
            //TODO
        });
        buttonCreateDockerfile.addActionListener(e -> {
            if (GitManager.isReady())
                DockerfileBuilder.createDockerfile(false);
        });
        buttonBuildBenchmarkImages.addActionListener(e -> {
            if (DockerManager.isReady()) {
                executorService.execute(() -> {
                    DockerManager.IsBuildingImage = true;
                    DockerManager.buildBenchmarkImages(false);
                    DockerManager.IsBuildingImage = false;
                });
                buttonBuildBenchmarkImages.setEnabled(false);
                Executors.newSingleThreadExecutor().execute(() -> {
                    StopWatch watch = new StopWatch();
                    watch.start();
                    while (DockerManager.IsBuildingImage) {
                        System.out.println("Image is building...");
                        System.out.println("Elapsed time: " + watch.getTime()/1000 + "s");
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /*
     *	Class to intercept output from a PrintStream and add it to a Document.
     *  The output can optionally be redirected to a different PrintStream.
     *  The text displayed in the Document can be color coded to indicate
     *  the output source.
     */
    private class ConsoleOutputStream extends ByteArrayOutputStream
    {
        private final String EOL = System.getProperty("line.separator");
        private SimpleAttributeSet attributes;
        private PrintStream printStream;
        private StringBuffer buffer = new StringBuffer(80);
        private boolean isFirstLine;

        /*
         *  Specify the option text color and PrintStream
         */
        public ConsoleOutputStream(Color textColor, PrintStream printStream)
        {
            if (textColor != null) {
                attributes = new SimpleAttributeSet();
                StyleConstants.setForeground(attributes, textColor);
            }

            this.printStream = printStream;
            isFirstLine = true;
        }

        /*
         *  Override this method to intercept the output text. Each line of text
         *  output will actually involve invoking this method twice:
         *
         *  a) for the actual text message
         *  b) for the newLine string
         *
         *  The message will be treated differently depending on whether the line
         *  will be appended or inserted into the Document
         */
        public void flush()
        {
            String message = toString();

            if (message.length() == 0) return;

            handleAppend(message);

            reset();
        }

        /*
         *	We don't want to have blank lines in the Document. The first line
         *  added will simply be the message. For additional lines it will be:
         *
         *  newLine + message
         */
        private void handleAppend(String message)
        {
            //  This check is needed in case the text in the Document has been
            //	cleared. The buffer may contain the EOL string from the previous
            //  message.

            if (document.getLength() == 0)
                buffer.setLength(0);

            if (EOL.equals(message)) {
                buffer.append(message);
            }
            else {
                buffer.append(message);
                clearBuffer();
            }

        }

        /*
         *  The message and the newLine have been added to the buffer in the
         *  appropriate order so we can now update the Document and send the
         *  text to the optional PrintStream.
         */
        private void clearBuffer()
        {
            //  In case both the standard out and standard err are being redirected
            //  we need to insert a newline character for the first line only

            if (isFirstLine && document.getLength() != 0) {
                buffer.insert(0, "\n");
            }

            isFirstLine = false;
            String line = buffer.toString();

            try {
                int offset = document.getLength();
                document.insertString(offset, line, attributes);
                textAreaConsole.setCaretPosition(document.getLength());
            } catch (BadLocationException ble) {
            }

            if (printStream != null) {
                printStream.print(line);
            }

            buffer.setLength(0);
        }
    }
}
