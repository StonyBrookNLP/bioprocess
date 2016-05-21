package edu.stanford.nlp.bioprocess;

import com.google.gson.*;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.util.Pair;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dick on 5/20/16.
 */

public class BioDatumDeserializer implements JsonDeserializer<BioDatumShell> {
    @Override
    public BioDatumShell deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        final JsonObject jsonObject = json.getAsJsonObject();
        String sentence = jsonObject.get("s").getAsString();
        String exid = jsonObject.get("exid").getAsString();
        String gold = jsonObject.get("gold").getAsString();
        String pred = jsonObject.get("pred").getAsString();
        String span = jsonObject.get("span").getAsString();
        int w0 = jsonObject.get("w0").getAsInt();
        int w1 = jsonObject.get("w1").getAsInt();
        List<Pair<String, Double>> ranked = new ArrayList<Pair<String, Double>>();
        JsonArray rroles = jsonObject.get("rroles").getAsJsonArray();
        JsonArray rprobs = jsonObject.get("rprobs").getAsJsonArray();
        for (int i=0; i<rroles.size(); ++i) {
            ranked.add(new Pair(rroles.get(i).getAsString(), rprobs.get(i).getAsDouble()));
        }
        return new BioDatumShell(sentence, exid, gold, pred, span, w0, w1, ranked);
    }
}
