package adbm.main;

import adbm.main.ui.MainWindow;
import adbm.util.AdbmConstants;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static adbm.util.helpers.FormatUtil.format;

public class AntidoteCommandLine
{
    private static final Logger log = LogManager.getLogger(AntidoteCommandLine.class);

    public static boolean run(String[] args)
    {
        //For Command Line
        if (args == null || args.length == 0) return false;
        //Declaring the options
        List<String> benchmarkCommits = new ArrayList<>();
        Option gui = new Option("gui", "activate gui mode");
        Option commits = Option.builder().argName("commits")
                               .hasArgs()
                               .desc("set the commits you want to benchmark and compare")
                               .longOpt("commits")
                               .build();

        Options options = new Options();

        //Adding the options
        options.addOption(gui);
        options.addOption(commits);

        //Parsing through the command line arguments
        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine line = parser.parse(options, args);

            //Option to activate gui
            if (line.hasOption("gui")) {
                Main.setGuiMode(true);
                Main.getSettingsManager().start();
                MainWindow.showMainWindow();
            }
            else if (line.hasOption("commits")) {
                Main.setGuiMode(false);
                for (String value : line.getOptionValues("commits")) {
                    if (value == null) {
                        log.warn(
                                "The commit id {} was not found in the repository and cannot be added benchmark!",
                                value);
                    }
                    else {
                        benchmarkCommits.add(value);
                    }
                }
                if (!benchmarkCommits.isEmpty()) {
                    Main.startBenchmarkContainer();
                    for (String commit : benchmarkCommits) {
                        boolean rebuildSuccess = Main.getDockerManager()
                                .rebuildAntidoteInContainer(AdbmConstants.ADBM_CONTAINER, commit);
                        if (!rebuildSuccess) {
                            log.error("Rebuild of Antidote in the container failed! The benchmark cannot be run!");
                        }
                        else {
                            //Calling the benchmark
                            Main.getBenchmarkConfig().runBenchmark();
                        }
                    }
                }
                else {
                    log.warn("No commit id () was found !");
                }
            }
        } catch (ParseException exp) {
            // oops, something went wrong
            log.error("Parsing failed.  Reason: " + exp.getMessage());
        }
        return true;
    }
}
