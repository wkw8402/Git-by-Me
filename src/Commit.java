package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import static gitlet.Utils.*;

public class Commit implements Serializable {
    /** Log message. */
    private String msg;
    /** Timestamp for creation. */
    private String time;
    /** UID of parent commit. */
    private String dad;
    /** UID of second parent commit. */
    private String mom;
    /** Keys with filename and values with sha1 of file contents. */
    private TreeMap<String, String> blobs;
    /** Current Working Directory. */
    private String uid;

    public Commit(String mess, String pa, String secPa) throws IOException {
        this.msg = mess;
        this.dad = pa;
        this.mom = secPa;
        String patt = "E MMM dd HH:mm:ss yyyy Z";
        SimpleDateFormat dateFormat = new SimpleDateFormat(patt);
        Date date;
        if (this.dad.equals("")) {
            date = new Date(0);
            this.time = dateFormat.format(date);
            this.blobs = new TreeMap<>();
        } else {
            date = new Date();
            this.time = dateFormat.format(date);
            File par = Utils.join(Main.COMMITS_FOLDER, sha1(pa));
            this.blobs = Utils.readObject(par, Commit.class).getBlobs();
            for (File f : Main.ADDITION.listFiles()) {
                this.blobs.put(f.getName(), sha1(Utils.readContents(f)));
                File blob = Utils.join(Main.BLOBS_FOLDER, sha1(Utils.readContents(f)));
                Utils.writeContents(blob, Utils.readContents(f));
            }
            for (File f : Main.REMOVAL.listFiles()) {
                this.blobs.remove(f.getName());
                File blob = Utils.join(Main.BLOBS_FOLDER, sha1(Utils.readContents(f)));
                blob.delete();
            }
        }
        this.uid = sha1(this.msg, this.time, this.dad, serialize(this.blobs));
    }

    public String getMsg() {
        return this.msg;
    }

    public String getTime() {
        return this.time;
    }

    public String getDad() {
        return this.dad;
    }

    public String getMom() {
        return this.mom;
    }

    public TreeMap<String, String> getBlobs() {
        return this.blobs;
    }

    public String getUid() {
        return this.uid;
    }
}
