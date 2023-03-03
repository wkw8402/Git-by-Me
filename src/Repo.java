package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Utils.sha1;

public class Repo {
    public File getBranches() {
        return branches;
    }

    public File getAddition() {
        return addition;
    }

    public File getRemoval() {
        return removal;
    }

    public File getCommits() {
        return commits;
    }

    /** Persisted branches folder. */
    private final File branches = Main.BRANCHES_FOLDER;
    /** Persisted addition folder. */
    private final File addition = Main.ADDITION;
    /**  Persisted removal folder. */
    private final File removal = Main.REMOVAL;
    /**  Persisted commits folder. */
    private final File commits = Main.COMMITS_FOLDER;
    /** Persisted file to store current branch name. */
    private File currentBranch = Utils.join(branches, "current_branch");

    public String getCurrentBranch() {
        return Utils.readContentsAsString(currentBranch);
    }

    public void setCurrentBranch(String branchName) throws IOException {
        Utils.writeContents(this.currentBranch, branchName);
    }

    Commit getHeadOfBranch(String branchName) {
        File branch = Utils.join(branches, branchName);
        String commitID = Utils.readContentsAsString(branch);
        File commit = Utils.join(commits, sha1(commitID));
        Commit found = Utils.readObject(commit, Commit.class);
        return found;
    }

    void makeNewBranch(String name, Commit current) {
        File branch = Utils.join(branches, name);
        Utils.writeContents(branch, current.getUid());
    }

    void updateBranch(File branch, Commit now) throws IOException {
        Utils.writeContents(branch, now.getUid());
    }

    void updateCommit(Commit commit) {
        File com = Utils.join(Main.COMMITS_FOLDER, sha1(commit.getUid()));
        Utils.writeObject(com, commit);
        clearStagingArea();
    }

    void clearStagingArea() {
        for (File staged : addition.listFiles()) {
            staged.delete();
        }
        for (File staged : removal.listFiles()) {
            staged.delete();
        }
    }
}
