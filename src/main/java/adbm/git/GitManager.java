package adbm.git;

import adbm.settings.MapDBManager;
import adbm.settings.ui.SettingsDialog;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GitManager
{
    private static Git git;

    private static final String gitUrl = "https://github.com/SyncFree/antidote.git";

    public static void startGit()
    {
        if (!MapDBManager.isReady()) return;
        try {
            String repoLocation = MapDBManager.getAppSetting(MapDBManager.GitRepoLocationSetting);
            if (repoLocation.equals("")) {
                System.out.println("No location for the git repository was selected!");
                System.out.println("Please select a valid location in the settings!");
                new SettingsDialog();
            }
            else {
                File directory = new File(repoLocation);
                File[] contents = directory.listFiles();
                if (contents == null) {
                    System.out.println("The location for the git repository is not a directory!");
                    System.out.println("Please select a valid location in the settings!");
                }
                try {
                    git = Git.open(new File(repoLocation));
                } catch (Exception e) {
                    git = null;
                }
                if (git != null) {
                    if (git.status().call().isClean()) {
                        String url = git.getRepository().getConfig().getString("remote", "origin", "url");
                        if (url.equals(gitUrl)) {
                            System.out.println("Git connection was successfully established!");
                            System.out.println(
                                    "This application does not yet automatically fetch remote changes and that must be done manually!");//TODO add fetch
                        }
                        else {
                            System.out.println(
                                    "The location for the git repository contains a another repository that is not equal to " + gitUrl + "!");
                            System.out.println(
                                    "Please select another location in the settings or remove that git repository first!");
                            git = null;
                        }
                    }
                    else {
                        System.out.println("The git repository is not clean!");
                        System.out.println("Please commit all changes before using this application!");
                        git = null;
                    }

                }
                else {
                    System.out.println("There is currently no git repository at the selected location!");
                    System.out.println(
                            "The git repository " + gitUrl + " will be cloned to the selected location if there are no files in that directory.");

                    if (contents.length == 0) {
                        System.out.println(
                                "Cloning the git repository " + gitUrl + " to the location " + repoLocation + "!");
                        git = Git.cloneRepository()
                                 .setURI(gitUrl)
                                 .setDirectory(new File(repoLocation))
                                 .setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)))
                                 .call();
                    }
                    else {
                        // TODO Add Setting that allows this!
                        System.out.println(
                                "The directory at selected location contains files and cannot be used as a git repository.");
                        System.out.println(
                                "Please select an empty directory in the settings or remove the existing files in that directory!");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkoutBranch(String branchName)
    {
        if (!isReady()) return;
        try {

            if (getAllLocalBranches().contains(branchName)) {
                git.checkout().
                        setName(branchName).
                           call();
                System.out.println("Checkout of local Branch " + branchName + " was successful!");
            }
            else if (getAllNonLocalRemoteBranches().contains(branchName)) {
                System.out.println(
                        "Local branch for the remote branch" + branchName + " does not exist and is added now!");
                git.checkout().
                        setCreateBranch(true).
                           setName(branchName).
                           setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).
                           setStartPoint("origin/" + branchName).
                           call();
                System.out.println("Local branch " + branchName + " was created and successfully checked out!");
            }
            else {
                System.out.println("Branch " + branchName + " could not be checked out!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void checkoutCommit(String commit)
    {
        if (!isReady()) return;
        try {
            System.out.println("Checking out commit " + commit + " and detaching HEAD!");
            git.checkout().setName(commit).call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCurrentBranch()
    {
        if (!isReady()) return "";
        try {
            String branchName = git.getRepository().getBranch();
            if (branchName == null) return "";
            return branchName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static RevCommit getCurrentCommit()
    {
        if (!isReady()) return null;
        try {
            ObjectId id = git.getRepository().resolve(Constants.HEAD);
            RevWalk walk = new RevWalk(git.getRepository());
            return walk.parseCommit(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> getAllLocalBranches()
    {
        List<String> list = new ArrayList<>();
        if (!isReady()) return list;
        try {
            List<Ref> branches = git.branchList().call();
            branches.forEach(branch -> list.add(branch.getName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllNonLocalRemoteBranches()
    {
        List<String> list = new ArrayList<>();
        if (!isReady()) return list;
        try {
            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
            List<String> localBranches = getAllLocalBranches();
            branches.forEach(branch -> {
                String shortName = git.getRepository().shortenRemoteBranchName(branch.getName());
                if (!localBranches.contains(shortName))
                    list.add(shortName);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static List<RevCommit> getCommitsForCurrentHead(int number)
    {
        List<RevCommit> list = new ArrayList<>();
        if (!isReady()) return list;
        try {
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterable<RevCommit> commits = git.log().add(head).setMaxCount(number).call();
            commits.forEach(list::add);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static RevCommit getCommitFromID(String id)
    {
        ObjectId commitId = ObjectId.fromString(id);
        RevWalk revWalk = new RevWalk(git.getRepository());
        try {
            RevCommit commit = revWalk.parseCommit(commitId);
            revWalk.close();
            return commit;
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("The ID " + id + " did not match any commits!");
        return null;
    }

    public static boolean isReady()
    {
        if (git != null) return true;
        System.out.println("The connection to Git is not ready!");
        System.out.println("Please start Git connection again!");
        return false;
    }

    public static boolean isReadyNoText()
    {
        if (git != null) return true;
        return false;
    }

    /*if (git == null) { TODO for later
            }
            else {
                RevWalk walk = new RevWalk(git.getRepository());

                List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

                for (Ref branch : branches) {
                    String branchName = branch.getName();
                    System.out.println("Commits of branch: " + branch.getName());
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");
                    System.out.println("---------------------------------");


                    Iterable<RevCommit> commits = git.log().all().call();

                    for (RevCommit commit : commits) {
                        boolean foundInThisBranch = false;

                        RevCommit targetCommit = walk.parseCommit(git.getRepository().resolve(
                                commit.getName()));
                        for (Map.Entry<String, Ref> e : git.getRepository().getAllRefs().entrySet()) {
                            if (e.getKey().startsWith(Constants.R_HEADS)) {
                                if (walk.isMergedInto(targetCommit, walk.parseCommit(
                                        e.getValue().getObjectId())))
                                {
                                    String foundInBranch = e.getValue().getName();
                                    if (branchName.equals(foundInBranch)) {
                                        foundInThisBranch = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (foundInThisBranch) {
                            System.out.println(commit.getName());
                            System.out.println(commit.getAuthorIdent().getName());
                            System.out.println(new Date((long)commit.getCommitTime()*1000));
                            System.out.println(commit.getFullMessage());
                        }
                    }
                }
            }*/
}
