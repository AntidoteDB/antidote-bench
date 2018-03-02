package adbm.main;

import adbm.antidote.IAntidoteClientWrapper;
import adbm.resultsVisualization.VisualizationMain;
import adbm.util.AdbmConstants;
import adbm.util.helpers.GeneralUtil;
import com.yahoo.ycsb.Client;
import eu.antidotedb.antidotepb.AntidotePB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static adbm.util.helpers.FormatUtil.format;

public class BenchmarkConfig
{
    private static final Logger log = LogManager.getLogger(BenchmarkConfig.class);

    private AntidotePB.CRDT_type usedKeyType = AntidotePB.CRDT_type.COUNTER;

    public AntidotePB.CRDT_type getUsedKeyType()
    {
        return usedKeyType;
    }

    private String usedOperation;

    public String getUsedOperation()
    {
        return usedOperation;
    }

    private IAntidoteClientWrapper.TransactionType usedTransactionType = IAntidoteClientWrapper.TransactionType.InteractiveTransaction;

    public IAntidoteClientWrapper.TransactionType getUsedTransactionType()
    {
        return usedTransactionType;
    }

    private boolean useTransactions = true;

    public void setUseTransactions(boolean bool)
    {
        useTransactions = bool;
    }

    public boolean getUseTransactions()
    {
        return useTransactions;
    }

    private String usedWorkload = "SampleWorkload";

    public String getUsedWorkLoad()
    {
        return usedWorkload;
    }

    public void setUsedWorkload(String workload)
    {
        usedWorkload = workload;
    }

    private int numberOfThreads = 1;

    public int getNumberOfThreads()
    {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int number)
    {
        if (number > 0) numberOfThreads = number;
    }

    private int targetNumber = 0;

    public int getTargetNumber()
    {
        return targetNumber;
    }

    public void setTargetNumber(int number)
    {
        if (number >= 0) targetNumber = number;
    }

    private boolean rebuildAntidote(String commit)
    {
        if (commit == null) return true;
        boolean rebuildSuccess = Main.getDockerManager()
                                     .rebuildAntidoteInContainer(AdbmConstants.benchmarkContainerName, commit);
        return rebuildSuccess;
    }

    public boolean runBenchmark(String... commits)
    {

        if (!Main.isDockerRunning) return false;
        String currentDateTime = new SimpleDateFormat(AdbmConstants.dateFormat).format(new Date());
        String usedDB = AdbmConstants.ycsbDBClassName;
        boolean showStatus = true;
        String[] threadsArg = numberOfThreads <= 1 ? new String[0] : new String[]{"-threads", format("{}",
                                                                                                     numberOfThreads)};
        String[] targetArg = targetNumber <= 0 ? new String[0] : new String[]{"-target", format("{}",
                                                                                                targetNumber)};
        String[] transactionArg = useTransactions ? new String[]{"-t"} : new String[0];
        String[] dbArg = {"-db", format("{}", usedDB)};
        String[] workloadArg = {"-P", format("{}/{}", AdbmConstants.ycsbWorkloadsPath, usedWorkload)};
        String[] statusArg = showStatus ? new String[]{"-s"} : new String[0];

        List<String> argList = new ArrayList<>();
        GeneralUtil.addIfNotEmpty(argList, threadsArg, targetArg, transactionArg, dbArg, workloadArg, statusArg);

        if (commits.length == 0) {
            commits = new String[]{null};
        }
        String fileName = format("{}/{}", AdbmConstants.ycsbResultsPath, AdbmConstants.resultFileName);
        String fileEnd = ".csv";
        List<String> resultFiles = new ArrayList<>();
        for (String commit : commits) {
            if (!rebuildAntidote(commit)) return false;
            if (commit == null) commit = Main.getDockerManager().getCommitOfContainer(AdbmConstants.benchmarkContainerName);
            String shortCommit = commit.substring(0, Math.min(commit.length(), AdbmConstants.numberCommitAbbreviation));
            String resultFileName = format("{}_{}_{}{}", fileName, shortCommit, currentDateTime, fileEnd);
            resultFiles.add(resultFileName);
            String[] resultFileArg = new String[]{"-p", format("exportfile={}", resultFileName)};
            List<String> newArgList = new ArrayList<>(argList);
            GeneralUtil.addIfNotEmpty(newArgList, resultFileArg);
            String[] ycsbArgs = newArgList.toArray(new String[0]);
            Client.main(ycsbArgs);
        }
        VisualizationMain.showResultsWindow(resultFiles.toArray(new String[0]));
        return true;
    }
}
