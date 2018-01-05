import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;

public class Member implements Serializable {

    private String ip;
    private int count;
    private int id;
    private Date latestUpdateTime;
    private boolean alive;

    public Member(String ip, int count) {
        this.ip = ip;
        this.count = count;
        this.id = findId(ip);
        this.latestUpdateTime = null;
        this.alive = true;
    }

    @Override
    public String toString() {
        String s = "Member: " + id + ", HeartBeats count: " + count +
                ", latestUpdateTime: " + latestUpdateTime +
                ", isAlive: " + alive + "\n";
        return s;
    }

    public int findId(String ip) {
        return Integer.valueOf(ip.substring(ip.length()-2, ip.length()))-1;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(ip);
        out.writeInt(count);
        out.writeInt(id);
        out.writeObject(latestUpdateTime);
        out.writeBoolean(alive);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        ip = (String)in.readObject();
        count = in.readInt();
        id = in.readInt();
        latestUpdateTime = (Date)in.readObject();
        alive = in.readBoolean();
    }

    public Date getLatestUpdateTime() {
        return latestUpdateTime;
    }

    public void setLatestUpdateTime(Date latestUpdateTime) {
        this.latestUpdateTime = latestUpdateTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
