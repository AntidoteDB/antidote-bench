package adbm.git;

import adbm.util.EverythingIsNonnullByDefault;
import adbm.util.IStartStop;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.Nullable;
import java.util.List;

/**
 *
 */
@EverythingIsNonnullByDefault
public interface IGitManager extends IStartStop
{

    /**
     *
     * @param branchName
     * @return
     */
    boolean checkoutBranch(String branchName);

    /**
     *
     * @param commitId
     * @return
     */
    boolean checkoutCommit(String commitId);

    /**
     *
     * @return
     */
    String getCurrentBranch();

    /**
     *
     * @return
     */
    @Nullable
    RevCommit getCurrentCommit();

    /**
     *
     * @return
     */
    List<String> getAllLocalBranches();

    /**
     *
     * @return
     */
    List<String> getAllNonLocalRemoteBranches();

    /**
     *
     * @param numberOfCommits
     * @return
     */
    List<RevCommit> getCommitsForCurrentHead(int numberOfCommits);

    /**
     *
     * @param commitId
     * @return
     */
    @Nullable
    RevCommit getCommitFromId(String commitId);

    /**
     *
     * @param commitId
     * @return
     */
    boolean isCommitId(String commitId);

}
