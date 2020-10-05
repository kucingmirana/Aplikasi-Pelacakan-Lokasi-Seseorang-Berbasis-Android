package com.example.peta.Notification;

public class Sender {
    public Data data;
    public String to;
    public String priority;
    public int numPrio;

    public Sender(Data data, String to, String priority, int numPrio) {
        this.data = data;
        this.to = to;
        this.priority = priority;
        this.numPrio = numPrio;
    }

    public Sender() {
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getNumPrio() {
        return numPrio;
    }

    public void setNumPrio(int numPrio) {
        this.numPrio = numPrio;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
