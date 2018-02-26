package adbm.settings;

import adbm.util.IStartStop;

import java.util.HashSet;

public interface ISettingsManager extends IStartStop
{

    String GitRepoLocationSetting = "GitRepo";

    String getAppSetting(String setting);

    boolean setAppSetting(String setting, String value);

    boolean resetAppSettings();

    String getGitRepoLocation();

    void setGitRepoLocation(String path);

    HashSet<String> getBenchmarkCommits();

    boolean addBenchmarkCommit(String commitHash);

    boolean removeBenchmarkCommit(String commitHash);

    boolean resetBenchmarkCommits();

}
