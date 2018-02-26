package adbm.git;

import adbm.util.IStartStop;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.List;

public interface IGitManager extends IStartStop
{

    boolean checkoutBranch(String branchName);

    boolean checkoutCommit(String commit);

    String getCurrentBranch();

    RevCommit getCurrentCommit();

    List<String> getAllLocalBranches();

    List<String> getAllNonLocalRemoteBranches();

    List<RevCommit> getCommitsForCurrentHead(int number);

    RevCommit getCommitFromID(String id);

    boolean isCommitId(String value);

}
