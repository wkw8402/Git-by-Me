package gitlet;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static gitlet.Utils.UID_LENGTH;
import static gitlet.Utils.sha1;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kyung-Wan Woo
 */
public class Main {

    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** Gitlet folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");
    /** Blobs folder. */
    static final File BLOBS_FOLDER = Utils.join(GITLET_FOLDER, "blobs");
    /** Branches folder. */
    static final File BRANCHES_FOLDER = Utils.join(GITLET_FOLDER, "branches");
    /** Staging area folder. */
    static final File STAGING_AREA = Utils.join(GITLET_FOLDER, "staging_area");
    /** Staged for addition folder. */
    static final File ADDITION = Utils.join(STAGING_AREA, "addition");
    /** Staged for removal folder. */
    static final File REMOVAL = Utils.join(STAGING_AREA, "removal");
    /** Commits folder. */
    static final File COMMITS_FOLDER = Utils.join(GITLET_FOLDER, "commits");
    /** Repo class's instance to keep track of this .gitlet directory. */
    static final Repo REPO = new Repo();

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        } else if (args[0].equals("init")) {
            init();
            System.exit(0);
        } else {
            noGitletCheck();
        }
        if (args[0].equals("add")) {
            add(args[1]);
        } else if (args[0].equals("commit")) {
            commit(args[1]);
        } else if (args[0].equals("rm")) {
            rm(args[1]);
        } else if (args[0].equals("log")) {
            log();
        } else if (args[0].equals("global-log")) {
            global();
        } else if (args[0].equals("find")) {
            find(args[1]);
        } else if (args[0].equals("checkout")) {
            filterCheckout(args);
        } else if (args[0].equals("status")) {
            status();
        } else if (args[0].equals("branch")) {
            branch(args[1]);
        } else if (args[0].equals("rm-branch")) {
            removeBranch(args[1]);
        } else if (args[0].equals("reset")) {
            reset(args[1]);
        } else if (args[0].equals("merge")) {
            merge(args[1]);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }

    private static void filterCheckout(String[] args) throws IOException {
        if (args[1].equals("--")) {
            checkoutFile(args[2]);
        } else if (args.length == 2) {
            if (isBranch(args[1])) {
                checkoutBranch(args[1]);
            } else {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
        } else if (args[2].equals("--")) {
            com(args[1], args[3]);
        } else if (!args[2].equals("--")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void setupPersistence() {
        GITLET_FOLDER.mkdir();
        COMMITS_FOLDER.mkdir();
        BRANCHES_FOLDER.mkdir();
        BLOBS_FOLDER.mkdir();
        STAGING_AREA.mkdir();
        ADDITION.mkdir();
        REMOVAL.mkdir();
    }

    private static Commit getCurrent() {
        File branch = Utils.join(BRANCHES_FOLDER, REPO.getCurrentBranch());
        String uid = Utils.readContentsAsString(branch);
        File commit = Utils.join(COMMITS_FOLDER, sha1(uid));
        Commit current = Utils.readObject(commit, Commit.class);
        return current;
    }

    private static void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            String m1 = "A Gitlet version-control system";
            String m2 = " already exists in the current directory.";
            System.out.println(m1 + m2);
            System.exit(0);
        }
        setupPersistence();
        Commit initial = new Commit("initial commit", "", "");
        REPO.updateCommit(initial);
        REPO.makeNewBranch("master", initial);
        REPO.setCurrentBranch("master");
    }

    private static void noGitletCheck() {
        if (!GITLET_FOLDER.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    private static void add(String fileName) throws IOException {
        File target = Utils.join(CWD, fileName);
        if (!target.isFile()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Commit current = getCurrent();

        if (current.getBlobs().keySet().contains(fileName)) {
            File tracked = Utils.join(BLOBS_FOLDER, current.getBlobs().get(fileName));
            String trackedContent = Utils.readContentsAsString(tracked);
            if (Utils.readContentsAsString(target).equals(trackedContent)) {
                Utils.join(REMOVAL, fileName).delete();
            } else {
                String fileContents = Utils.readContentsAsString(target);
                File toAdd = Utils.join(ADDITION, fileName);
                Utils.writeContents(toAdd, fileContents);
            }
        } else {
            String fileContents = Utils.readContentsAsString(target);
            File toAdd = Utils.join(ADDITION, fileName);
            Utils.writeContents(toAdd, fileContents);
        }
    }

    private static void commit(String message) throws IOException {
        if (ADDITION.listFiles().length == 0 && REMOVAL.listFiles().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        if (message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        File branch = Utils.join(BRANCHES_FOLDER, REPO.getCurrentBranch());
        String uid = Utils.readContentsAsString(branch);
        Commit now = new Commit(message, uid, "");
        REPO.updateCommit(now);
        REPO.updateBranch(branch, now);
        REPO.clearStagingArea();
    }

    private static void rm(String filename) throws IOException {
        Commit current = getCurrent();
        boolean notTracked = !current.getBlobs().keySet().contains(filename);
        if (notTracked && !stagedToAdd(filename)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        File toAdd = Utils.join(ADDITION, filename);
        if (toAdd.isFile()) {
            toAdd.delete();
        }
        if (current.getBlobs().keySet().contains(filename)) {
            File target = Utils.join(CWD, filename);
            if (target.isFile()) {
                target.delete();
            }
            File toRemove = Utils.join(REMOVAL, filename);
            toRemove.createNewFile();
        }
    }

    private static boolean stagedToAdd(String filename) {
        boolean staged = false;
        for (File added : ADDITION.listFiles()) {
            if (added.getName().equals(filename)) {
                staged = true;
            }
        }
        return staged;
    }

    private static boolean stagedToRemove(String filename) {
        boolean staged = false;
        for (File removed : REMOVAL.listFiles()) {
            if (removed.getName().equals(filename)) {
                staged = true;
            }
        }
        return staged;
    }

    private static void log() {
        Commit current = getCurrent();
        while (!current.getDad().equals("")) {
            printLog(current);
            current = getParent(current);
        }
        printLog(current);
    }

    private static void printLog(Commit current) {
        System.out.println("===");
        System.out.println("commit " + current.getUid());
        System.out.println("Date: " + current.getTime());
        System.out.println(current.getMsg());
        System.out.println();
    }

    private static void global() {
        for (File commits : COMMITS_FOLDER.listFiles()) {
            Commit each = Utils.readObject(commits, Commit.class);
            printLog(each);
        }
    }

    private static void find(String message) {
        boolean found = false;
        for (File commits : COMMITS_FOLDER.listFiles()) {
            Commit each = Utils.readObject(commits, Commit.class);
            if (each.getMsg().equals(message)) {
                System.out.println(each.getUid());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    private static void status() {
        statusBranch();
        statusStaged();
        statusRemoved();
        System.out.println("=== Modifications Not Staged For Commit ===");
        Commit current = getCurrent();
        ArrayList<String> modifications = new ArrayList<>();
        for (String fileName : current.getBlobs().keySet()) {
            File inCWD = Utils.join(CWD, fileName);
            if (!inCWD.isFile() && !stagedToRemove(fileName)) {
                modifications.add(fileName + " (deleted)");
            }
            if (inCWD.isFile()) {
                String fName = current.getBlobs().get(fileName);
                File inBlobsFolder = Utils.join(BLOBS_FOLDER, fName);
                String contentCWD = Utils.readContentsAsString(inCWD);
                String contentBlob = Utils.readContentsAsString(inBlobsFolder);
                if (!contentCWD.equals(contentBlob) && !stagedToAdd(fileName)) {
                    modifications.add(fileName + " (modified)");
                }
            }
        }
        for (File added : ADDITION.listFiles()) {
            File inCWD = Utils.join(CWD, added.getName());
            if (!inCWD.isFile()) {
                modifications.add(added.getName() + " (deleted)");
            }
            String contentAdded = Utils.readContentsAsString(added);
            String contentCWD = Utils.readContentsAsString(inCWD);
            if (!contentAdded.equals(contentCWD)) {
                modifications.add(added.getName() + " (modified)");
            }
        }
        Collections.sort(modifications);
        for (String mod : modifications) {
            System.out.println(mod);
        }
        System.out.println();
        statusUntracked(current);
    }

    private static void statusUntracked(Commit current) {
        System.out.println("=== Untracked Files ===");
        ArrayList<String> untracked = new ArrayList<>();
        for (File inCWD : CWD.listFiles()) {
            boolean sr = !stagedToRemove(inCWD.getName());
            boolean sa = !stagedToAdd(inCWD.getName());
            boolean tr = !current.getBlobs().keySet().contains(inCWD.getName());
            if (sr && sa && tr && notgit(inCWD)) {
                untracked.add(inCWD.getName());
            }
        }
        Collections.sort(untracked);
        for (String un : untracked) {
            System.out.println(un);
        }
    }

    private static void statusRemoved() {
        System.out.println("=== Removed Files ===");
        for (String removed : sortFileNames(REMOVAL)) {
            System.out.println(removed);
        }
        System.out.println();
    }

    private static void statusStaged() {
        System.out.println("=== Staged Files ===");
        for (String added : sortFileNames(ADDITION)) {
            System.out.println(added);
        }
        System.out.println();
    }

    private static void statusBranch() {
        System.out.println("=== Branches ===");
        ArrayList<String> branchNames = sortFileNames(BRANCHES_FOLDER);
        branchNames.remove("current_branch");
        for (String branch : branchNames) {
            if (branch.equals(REPO.getCurrentBranch())) {
                System.out.print("*");
            }
            System.out.println(branch);
        }
        System.out.println();
    }

    private static ArrayList<String> sortFileNames(File directory) {
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : directory.listFiles()) {
            fileNames.add(file.getName());
        }
        Collections.sort(fileNames);
        return fileNames;
    }

    private static void checkoutFile(String f) throws FileNotFoundException {
        Commit current = getCurrent();
        checkout(f, current);
    }

    private static void com(String id, String f) throws FileNotFoundException {
        id = abbreviated(id);
        Commit current = commitExists(id);
        checkout(f, current);
    }

    private static String abbreviated(String commitID) {
        if (commitID.length() < UID_LENGTH) {
            for (File commit : COMMITS_FOLDER.listFiles()) {
                String uid = Utils.readObject(commit, Commit.class).getUid();
                if (uid.startsWith(commitID)) {
                    commitID = uid;
                }
            }
        }
        return commitID;
    }

    private static Commit commitExists(String commitID) {
        File commit = Utils.join(COMMITS_FOLDER, sha1(commitID));
        if (!commit.isFile()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return Utils.readObject(commit, Commit.class);
    }

    private static void checkoutBranch(String branchname) throws IOException {
        if (isCurrentBranch(branchname)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        Commit current = getCurrent();
        for (File untracked : CWD.listFiles()) {
            String name = untracked.getName();
            boolean nTracked = !current.getBlobs().keySet().contains(name);
            if (nTracked && notgit(untracked)) {
                String m1 = "There is an untracked file in the way; ";
                String m2 = "delete it, or add and commit it first.";
                System.out.println(m1 + m2);
                System.exit(0);
            }
        }
        File target = Utils.join(BRANCHES_FOLDER, branchname);
        String content = Utils.readContentsAsString(target);
        File found = Utils.join(COMMITS_FOLDER, sha1(content));
        Commit head = Utils.readObject(found, Commit.class);
        for (String filename : head.getBlobs().keySet()) {
            com(Utils.readContentsAsString(target), filename);
        }
        for (File stranger : CWD.listFiles()) {
            boolean nt = !head.getBlobs().keySet().contains(stranger.getName());
            if (nt && notgit(stranger)) {
                Utils.restrictedDelete(stranger);
            }
        }
        REPO.setCurrentBranch(branchname);
        REPO.clearStagingArea();
    }

    private static boolean notgit(File inCWD) {
        return !inCWD.getName().equals(".gitlet");
    }

    private static boolean isBranch(String branchname) {
        for (File branch : BRANCHES_FOLDER.listFiles()) {
            if (branch.getName().equals(branchname)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCurrentBranch(String branchname) {
        return REPO.getCurrentBranch().equals(branchname);
    }

    private static void checkout(String f, Commit c) {
        fileInCommit(f, c);
        String target = c.getBlobs().get(f);
        File blob = Utils.join(BLOBS_FOLDER, target);
        String contents = Utils.readContentsAsString(blob);
        File victim = Utils.join(CWD, f);
        Utils.writeContents(victim, contents);
    }

    private static void fileInCommit(String filename, Commit current) {
        if (!current.getBlobs().keySet().contains(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    private static void branch(String branchname) throws IOException {
        for (File branch : BRANCHES_FOLDER.listFiles()) {
            if (branch.getName().equals(branchname)) {
                System.out.println("A branch with that name already exists.");
                System.exit(0);
            }
        }
        REPO.makeNewBranch(branchname, getCurrent());
    }

    private static void removeBranch(String branchname) {
        if (!isBranch(branchname)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (REPO.getCurrentBranch().equals(branchname)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        File target = Utils.join(BRANCHES_FOLDER, branchname);
        target.delete();
    }

    private static void reset(String commitID) throws IOException {
        Commit target = commitExists(abbreviated(commitID));
        Commit current = getCurrent();
        for (File name : CWD.listFiles()) {
            boolean ntc = !current.getBlobs().keySet().contains(name.getName());
            boolean tct = target.getBlobs().keySet().contains(name.getName());
            if (ntc && tct && notgit(name)) {
                String m1 = "There is an untracked file in the way;";
                String m2 = " delete it, or add and commit it first.";
                System.out.println(m1 + m2);
                System.exit(0);
            }
        }
        for (String tracked : target.getBlobs().keySet()) {
            checkout(tracked, target);
        }
        for (File stranger : CWD.listFiles()) {
            String n = stranger.getName();
            boolean ntc = !target.getBlobs().keySet().contains(n);
            if (ntc && notgit(stranger)) {
                stranger.delete();
            }
        }
        File branch = Utils.join(BRANCHES_FOLDER, REPO.getCurrentBranch());
        REPO.updateBranch(branch, target);
        REPO.clearStagingArea();
    }

    private static void merge(String mBranch) throws IOException {
        f0(mBranch);
        Commit merging = REPO.getHeadOfBranch(mBranch);
        HashSet<String> ancs = getAncestorsOfMergingBranch(merging);
        Commit current = getCurrent();
        Commit split = getSplit(ancs, current);
        f1(mBranch, current, split);
        f2(merging, split);
        HashSet<String> addition = new HashSet<>();
        HashSet<String> removal = new HashSet<>();
        HashSet<String> conflict = new HashSet<>();
        for (String f : split.getBlobs().keySet()) {
            boolean cMod = modified(current, split, f);
            boolean mDel = deleted(merging, f);
            if (!cMod && mDel) {
                removal.add(f);
            } else if (deleted(current, f) && mDel) {
                removal.add(f);
            } else if (deleted(current, f) && !modified(merging, split, f)) {
                removal.add(f);
            } else if (modified(merging, split, f) && !cMod) {
                addition.add(f);
            } else if (modified(merging, split, f) && cMod) {
                if (diffContent(current, merging, f)) {
                    conflict.add(f);
                }
            }
        }
        for (String fileName : merging.getBlobs().keySet()) {
            if (deleted(current, fileName) && deleted(split, fileName)) {
                addition.add(fileName);
            } else if (deleted(split, fileName)) {
                if (modified(merging, current, fileName)) {
                    conflict.add(fileName);
                }
            }
        }
        for (File inCWD : CWD.listFiles()) {
            boolean add = addition.contains(inCWD.getName());
            boolean rem = removal.contains(inCWD.getName());
            boolean conf = conflict.contains(inCWD.getName());
            boolean influencedByMerge = add | rem | conf;
            boolean nt = !current.getBlobs().keySet().contains(inCWD.getName());
            if (notgit(inCWD) && nt && influencedByMerge) {
                untracked();
            }
        }
        plus(merging, addition);
        minus(current, removal);
        printMM(conflict);
        cf(merging, current, conflict);
        cm(mBranch, merging, current);
    }

    private static void cf(Commit m, Commit c, HashSet<String> cf) {
        for (String toConflict : cf) {
            File inCWD = Utils.join(CWD, toConflict);
            String inH = "";
            String inM = "";
            if (!deleted(c, toConflict)) {
                String name = c.getBlobs().get(toConflict);
                File blob = Utils.join(BLOBS_FOLDER, name);
                inH = Utils.readContentsAsString(blob);
            }
            if (!deleted(m, toConflict)) {
                String name = m.getBlobs().get(toConflict);
                File blob = Utils.join(BLOBS_FOLDER, name);
                inM = Utils.readContentsAsString(blob);
            }
            writeM(inCWD, inH, inM);
            try {
                add(inCWD.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void minus(Commit c, HashSet<String> r) throws IOException {
        for (String toRemove : r) {
            boolean inCwd = Utils.join(CWD, toRemove).isFile();
            boolean tr = c.getBlobs().containsKey(toRemove);
            if (inCwd && tr) {
                rm(toRemove);
            }
        }
    }

    private static void plus(Commit m, HashSet<String> a) throws IOException {
        for (String plus : a) {
            File inCWD = Utils.join(CWD, plus);
            File blob = Utils.join(BLOBS_FOLDER, m.getBlobs().get(plus));
            Utils.writeContents(inCWD, Utils.readContentsAsString(blob));
            add(plus);
        }
    }

    private static void cm(String mb, Commit m, Commit c) throws IOException {
        String m1 = "Merged " + mb + " into ";
        String m2 = REPO.getCurrentBranch() + ".";
        Commit merge = new Commit(m1 + m2, c.getUid(), m.getUid());
        REPO.updateCommit(merge);
        REPO.updateBranch(Utils.join(BRANCHES_FOLDER, REPO.getCurrentBranch()), merge);
    }

    private static void printMM(HashSet<String> conflict) {
        if (conflict.size() > 0) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private static void untracked() {
        String m1 = "There is an untracked file in the way;";
        String m2 = " delete it, or add and commit it first.";
        System.out.println(m1 + m2);
        System.exit(0);
    }

    private static void writeM(File inCWD, String inH, String inM) {
        String start = "<<<<<<< HEAD\n";
        String mid = "=======\n";
        String end = ">>>>>>>\n";
        Utils.writeContents(inCWD, start + inH + mid + inM + end);
    }

    private static void f0(String mb) {
        if (!isBranch(mb)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (isCurrentBranch(mb)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (ADDITION.listFiles().length > 0 | REMOVAL.listFiles().length > 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
    }

    private static void f1(String mb, Commit c, Commit s) throws IOException {

        if (s.getUid().equals(c.getUid())) {
            checkoutBranch(mb);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
    }

    private static void f2(Commit m, Commit s) {
        if (s.getUid().equals(m.getUid())) {
            String m1 = "Given branch is an ancestor";
            System.out.println(m1 + " of the current branch.");
            System.exit(0);
        }
    }

    private static boolean modified(Commit now,  Commit split, String fileName) {
        String inSplit = split.getBlobs().get(fileName);
        return !inSplit.equals(now.getBlobs().get(fileName));
    }

    private static boolean deleted(Commit now, String fileName) {
        return !now.getBlobs().containsKey(fileName);
    }

    private static boolean diffContent(Commit one, Commit two, String name) {
        if (deleted(one, name) | deleted(two, name)) {
            return true;
        }
        String tn = two.getBlobs().get(name);
        return !one.getBlobs().get(name).equals(tn);
    }

    private static Commit getSplit(HashSet<String> ancestors, Commit current) {
        Commit split = null;
        if (ancestors.contains(current.getUid())) {
            split = current;
            return split;
        }
        while (!current.getMsg().equals("initial commit")) {
            if (ancestors.contains(current.getDad())) {
                return getParent(current);
            } else if (ancestors.contains(current.getMom())) {
                return getSecondParent(current);
            }
            current = getParent(current);
        }
        if (split == null) {
            split = current;
        }
        return split;
    }

    private static Commit getParent(Commit current) {
        File parent = Utils.join(COMMITS_FOLDER, sha1(current.getDad()));
        current = Utils.readObject(parent, Commit.class);
        return current;
    }

    private static Commit getSecondParent(Commit current) {
        File secondParent = Utils.join(COMMITS_FOLDER, sha1(current.getMom()));
        current = Utils.readObject(secondParent, Commit.class);
        return current;
    }

    private static HashSet<String> getAncestorsOfMergingBranch(Commit target) {
        HashSet<String> ancestors = new HashSet<>();
        ancestors.add(target.getUid());
        while (!target.getMsg().equals("initial commit")) {
            ancestors.add(target.getDad());
            if (!target.getMom().equals("")) {
                ancestors.add(target.getMom());
            }
            target = getParent(target);
        }
        return ancestors;
    }
}