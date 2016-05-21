package edu.stanford.nlp.bioprocess;

import edu.stanford.nlp.util.Pair;

import java.io.Serializable;
import java.util.List;

/**
 * Created by dick on 5/20/16.
 */
public class BioDatumShell implements Serializable {
    String sentence;
    String exid;
    String gold;
    String pred;
    String span;
    int w0;
    int w1;
    List<Pair<String, Double>> ranked;

    public BioDatumShell(String sentence, String exid, String gold, String pred, String span,
                         int w0, int w1, List<Pair<String, Double>> ranked) {
        this.sentence = sentence;
        this.exid = exid;
        this.gold = gold;
        this.pred = pred;
        this.span = span;
        this.w0 = w0;
        this.w1 = w1;
        this.ranked = ranked;
    }
}
