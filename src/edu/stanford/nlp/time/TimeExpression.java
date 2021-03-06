package edu.stanford.nlp.time;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.MatchedExpression;
import edu.stanford.nlp.ling.tokensregex.types.Expressions;
import edu.stanford.nlp.ling.tokensregex.types.Value;
import edu.stanford.nlp.pipeline.CoreMapAttributeAggregator;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.TypesafeMap;

import java.util.Collections;
import java.util.List;

/**
 * Time Expression
 *
 * @author Angel Chang
 */
public class TimeExpression extends MatchedExpression {
  /**
   * The CoreMap key for storing a TimeExpression annotation
   */
  public static class Annotation implements CoreAnnotation<TimeExpression> {
    public Class<TimeExpression> getType() {
      return TimeExpression.class;
    }
  }

  /**
   * The CoreMap key for storing a nested annotations
   */
  public static class ChildrenAnnotation implements CoreAnnotation<List<? extends CoreMap>> {
    public Class<List<? extends CoreMap>> getType() {
      return ErasureUtils.<Class<List<? extends CoreMap>>> uncheckedCast(List.class);
    }
  }
  //int tid;     // Time ID

  SUTime.Temporal origTemporal;
  //int anchorTimeId = -1;

  public TimeExpression(MatchedExpression expr)
  {
    super(expr);
  }

  public TimeExpression(Interval<Integer> charOffsets, Interval<Integer> tokenOffsets,
                        Function<CoreMap, SUTime.Temporal> temporalFunc, double priority, double weight)
  {
    super(charOffsets, tokenOffsets, getSingleAnnotationExtractor(temporalFunc), priority, weight);
  }
  
  protected static final Function<MatchedExpression, TimeExpression> TimeExpressionConverter = new Function<MatchedExpression, TimeExpression>() {
    public TimeExpression apply(MatchedExpression in) {
      if (in == null) return null;
      if (in instanceof TimeExpression) return (TimeExpression) in;
      return new TimeExpression(in);
    }
  };
  
  private static SingleAnnotationExtractor getSingleAnnotationExtractor(final Function<CoreMap, SUTime.Temporal> temporalFunc)
  {
    SingleAnnotationExtractor extractFunc = new SingleAnnotationExtractor();
    extractFunc.valueExtractor = new Function<CoreMap, Value>() {
      public Value apply(CoreMap in) {
        SUTime.Temporal t = temporalFunc.apply(in);
        return new Expressions.PrimitiveValue("Temporal", t);
      }
    };
    extractFunc.tokensAnnotationField = CoreAnnotations.NumerizedTokensAnnotation.class;
    extractFunc.resultAnnotationField = Collections.singletonList((Class) TimeExpression.Annotation.class);
    extractFunc.resultNestedAnnotationField = TimeExpression.ChildrenAnnotation.class;
    extractFunc.resultAnnotationExtractor = TimeExpressionConverter;
    extractFunc.tokensAggregators = CoreMapAttributeAggregator.DEFAULT_NUMERIC_TOKENS_AGGREGATORS;
    return extractFunc;
  }

  public boolean addMod()
  {
    SUTime.Temporal t = getTemporal();
    if (t != null) {
      if (t != SUTime.TIME_NONE_OK) {
        setTemporal(EnglishTimeExpressionPatterns.addMod(text, t));
        return true;
      } else {
        return false;
      }
    } else {
      return true;
    }
  }

  public boolean extractAnnotation(Env env, CoreMap sourceAnnotation)
  {
    boolean okay = super.extractAnnotation(env, sourceAnnotation);
            //super.extractAnnotation(sourceAnnotation, CoreAnnotations.NumerizedTokensAnnotation.class,
            //CoreMapAttributeAggregator.DEFAULT_NUMERIC_TOKENS_AGGREGATORS,
            //TimeExpression.Annotation.class, TimeExpression.ChildrenAnnotation.class);
    if (okay) {
      return addMod();
    } else {
      return false;
    }
  }

  public boolean extractAnnotation(Env env, List<? extends CoreMap> source)
  {
    boolean okay = super.extractAnnotation(env, source);
            //super.extractAnnotation(source, CoreMapAttributeAggregator.getDefaultAggregators(),
            //TimeExpression.Annotation.class, TimeExpression.ChildrenAnnotation.class);
    if (okay) {
      return addMod();
    } else {
      return false;
    }
  }

 /* public int getTid() {
    return tid;
  }*/

  public SUTime.Temporal getTemporal() {
    if (value != null && value.get() instanceof SUTime.Temporal) {
      return (SUTime.Temporal) value.get();
    }
    return null;
  }
  public void setTemporal(SUTime.Temporal temporal) {
    this.value = new Expressions.PrimitiveValue("Temporal", temporal);
  }

/*  public String toString()
  {
    return text;
  } */

/*  public Timex getTimex(SUTime.TimeIndex timeIndex) {
    Timex timex = temporal.getTimex(timeIndex);
    timex.text = text;
    timex.xml = timex
    assert(timex.tid == tid);
  } */


}
