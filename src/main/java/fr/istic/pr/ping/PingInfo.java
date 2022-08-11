package fr.istic.pr.ping;

public class PingInfo {
    // Le temps de réponse :
    private long time;
    // Le code de réponse :
    private int code;

    public void setTime(long time) {
        this.time = time;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "[Code " + code + " in " + time + "ms]";
    }
}