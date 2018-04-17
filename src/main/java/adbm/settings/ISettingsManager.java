package adbm.settings;

import adbm.util.IStartStop;

import java.util.HashSet;

/**
 *
 */
public interface ISettingsManager extends IStartStop
{

    /**
     *
     */
    String GitRepoLocationSetting = "GitRepo";

    /**
     * @param settingName
     * @return
     */
    String getYCSBSetting(String settingName);

    /**
     * @param settingName
     * @param value
     * @return
     */
    boolean setYCSBSetting(String settingName, String value);

    /**
     *
     * @param settingName
     * @return
     */
    String getAppSetting(String settingName);

    /**
     *
     * @param settingName
     * @param value
     * @return
     */
    boolean setAppSetting(String settingName, String value);

    /**
     *
     * @return
     */
    boolean resetAppSettings();

    /**
     *
     * @return
     */
    String getGitRepoLocation();

    /**
     *
     * @param path
     */
    boolean setGitRepoLocation(String path);

    /**
     *
     * @return
     */
    HashSet<String> getBenchmarkCommits();

    /**
     *
     * @param commitId
     * @return
     */
    boolean addBenchmarkCommit(String commitId);

    /**
     *
     * @param commitId
     * @return
     */
    boolean removeBenchmarkCommit(String commitId);

    /**
     *
     * @return
     */
    boolean resetBenchmarkCommits();

}
