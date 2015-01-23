package uk.ac.lancs.aurorawatch;

import java.util.Date;

public class AuroraWatchXml implements Cloneable {
    State currentState;
    State previousState;
    public String station = "";
    public Date updated;

    public AuroraWatchXml() {
        currentState = new State();
        previousState = new State();
    }

    public String toString() {
        return "current state: " + currentState.toString() + " previous state: " + previousState.toString() +
                " station: " + station + " updated: " + (updated == null ? "(null)" : updated.toGMTString());
    }

    public AuroraWatchXml clone() throws CloneNotSupportedException {
        AuroraWatchXml r;
        r = (AuroraWatchXml) super.clone();
        r.updated = (Date) this.updated.clone();
        return r;
    }

    static public class State {
        String name;
        int value;
        int color;
        String description;

        public String toString() {
            return "name=" + name +
                    " value=" + Integer.toString(value) +
                    " color=#" + Integer.toHexString(color) +
                    " description=" + description;

        }
    }

}
