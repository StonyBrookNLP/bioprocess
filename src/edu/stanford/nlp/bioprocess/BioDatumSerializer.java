package edu.stanford.nlp.bioprocess;

import com.google.gson.*;
import edu.stanford.nlp.util.Pair;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dick on 5/20/16.
 */

public class BioDatumSerializer implements JsonSerializer<BioDatum> {
    public interface Func<In, Out> {
        Out apply(In in);
    }

    public <In, Out> List<Out> getElem(List<In> in, Func<In, Out> f) {
        List<Out> out = new ArrayList<Out>(in.size());
        for (In inObj : in) {
            out.add(f.apply(inObj));
        }
        return out;
    }

    @Override
    public JsonElement serialize(BioDatum d, Type type, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        Gson gson = new Gson();
        result.add("s", new JsonPrimitive(d.getSentence().toString()));
        result.add("exid", new JsonPrimitive(d.getExampleID()));
        result.add("gold", new JsonPrimitive(d.role));
        result.add("pred", new JsonPrimitive(d.guessRole));
        result.add("span", new JsonPrimitive(d.word));
        result.add("w0", new JsonPrimitive(d.entityNode.getSpan().getSource()));
        result.add("w1", new JsonPrimitive(d.entityNode.getSpan().getTarget()));

        List<Pair<String, Double>> ranked = d.getRankedRoles();
        List<String> rroles = getElem(ranked, new Func<Pair<String, Double>, String>() {
            public String apply(final Pair<String, Double> pair) {
                return pair.first;
            }
        });
        List<Double> rprobs = getElem(ranked, new Func<Pair<String, Double>, Double>() {
            public Double apply(final Pair<String, Double> pair) {
                return pair.second;
            }
        });
        JsonArray arr = new JsonArray();
        for (String role : rroles)
            arr.add(role);
        result.add("rroles", arr);
        arr = new JsonArray();
        for (Double prob : rprobs)
            arr.add(prob);
        result.add("rprobs", arr);
        return result;
    }
}
