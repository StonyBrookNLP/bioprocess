package edu.stanford.nlp.bioprocess.scripts;

import java.util.HashMap;

/**
 * Created by dick on 5/21/16.
 */
public class ParamOne {
    private static ParamOne instance = null;
    private HashMap<String, String> map;

    protected ParamOne() {
        map = new HashMap<String, String>();
    }

    public static ParamOne getInstance() {
        if (instance == null) {
            instance = new ParamOne();
        }
        return instance;
    }

    public void puts(String k, String v) {
        map.put(k, v);
    }
    public void putb(String k, boolean v) {
        map.put(k, Boolean.toString(v));
    }

    public String gets(String k) {
        return map.get(k);
    }
    public boolean getb(String k) {
        return Boolean.parseBoolean(map.get(k));
    }
}
