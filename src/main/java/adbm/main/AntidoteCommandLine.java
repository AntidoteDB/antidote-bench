package adbm.main;

import adbm.util.AdbmConstants;
import com.yahoo.ycsb.Client;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static adbm.util.helpers.FormatUtil.format;

public class AntidoteCommandLine
{
    private static final Logger log = LogManager.getLogger(AntidoteCommandLine.class);

    public static boolean run(String[] args) {
        //For Command Line
        if (args == null || args.length == 0) return false;

        return true;
    }
}
