package edu.stanford.nlp.bioprocess;

import java.util.List;

import edu.stanford.nlp.bioprocess.scripts.ParamOne;
import edu.stanford.nlp.classify.*;
import edu.stanford.nlp.ling.BasicDatum;


/***
 * Class that does the learning
 *
 * @author Aju
 */

public class Learner {
    //Parameters used by the model
    Params parameters;
    //List of examples used to learn the parameters
    List<Example> dataset;
    //Maximum number of iterations to be run
    final static int maxIterations = 100;

    /***
     * Constructor to initialize the Learner with a list of training examples.
     *
     * @param ds - List of training examples to learn the parameters from.
     */
    public Learner() {
        parameters = new Params();
    }

    /***
     * Method that will learn parameters for the model and return it.
     *
     * @return Parameters learnt.
     */
    public Params learn(List<Example> dataset, FeatureExtractor ff) {
        List<BioDatum> data = ff.setFeaturesTrain(dataset);
        boolean argid = ParamOne.getInstance().getb("useArgid");

        GeneralDataset<String, String> dd = new Dataset<String, String>();
        for (BioDatum d : data) {
            String role = d.role();
            if (argid) {
                if (role != "NONE")
                    role = ArgumentRelation.RelationType.Agent.toString();
            }
            dd.add(new BasicDatum<String, String>(d.features.getFeatures(), role));
        }
        if (argid) {
            LogisticClassifierFactory<String, String> lcFactory = new LogisticClassifierFactory<String, String>();
            LogisticClassifier<String, String> classifier = lcFactory.trainClassifier(dd);
            double [][] weights = { classifier.getWeights().clone(), classifier.getWeights().clone() } ;
            for (int i = 0; i<weights[1].length; i++) {
                weights[0][i] *= -1.0;
            }
            parameters.setWeights(weights);
        } else {
            LinearClassifierFactory<String, String> lcFactory = new LinearClassifierFactory<String, String>();
            LinearClassifier<String, String> classifier = lcFactory.trainClassifier(dd);
            parameters.setWeights(classifier.weights());
        }

        //parameters.setWeights(classifier.weights());
        parameters.setFeatureIndex(dd.featureIndex);
        parameters.setLabelIndex(dd.labelIndex);
        return parameters;
    }

    public Params learn(List<Example> dataset, EventRelationFeatureFactory ff) {
        List<BioDatum> data = ff.setFeaturesTrain(dataset);

        GeneralDataset<String, String> dd = new Dataset<String, String>();
        for (BioDatum d : data) {
            dd.add(new BasicDatum<String, String>(d.features.getFeatures(), d.label()));
        }

        LinearClassifierFactory<String, String> lcFactory = new LinearClassifierFactory<String, String>();
        LinearClassifier<String, String> classifier = lcFactory.trainClassifier(dd);

        parameters.setWeights(classifier.weights());
        parameters.setFeatureIndex(dd.featureIndex);
        parameters.setLabelIndex(dd.labelIndex);
        return parameters;
    }
}
