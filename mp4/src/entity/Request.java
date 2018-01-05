package entity;

import java.io.Serializable;

public class Request implements Serializable {
    private String action;
    private Object data;
    private int TTL;

    public Request(String action, Object data, int TTL) {
        this.action = action;
        this.data = data;
        this.TTL = TTL;
    }

    public Request(String action, Object data) {
        this.action = action;
        this.data = data;
        this.TTL = 0;
    }

    public int getTTL() {
        return TTL;
    }

    public void setTTL(int TTL) {
        this.TTL = TTL;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
