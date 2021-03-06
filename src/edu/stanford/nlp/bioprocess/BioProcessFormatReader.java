package edu.stanford.nlp.bioprocess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.stanford.nlp.bioprocess.ArgumentRelation.EventType;
import edu.stanford.nlp.bioprocess.BioProcessAnnotations.EntityMentionsAnnotation;
import edu.stanford.nlp.bioprocess.BioProcessAnnotations.EventMentionsAnnotation;
import edu.stanford.nlp.bioprocess.ArgumentRelation.RelationType;
import edu.stanford.nlp.ie.machinereading.GenericDataSetReader;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;

import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Triple;
import fig.basic.LogInfo;

public class BioProcessFormatReader extends GenericDataSetReader {
  protected static final String TEXT_EXTENSION = ".txt";
  protected static final String ANNOTATION_EXTENSION = ".ann";

  protected static final String THEME_TYPE_PREFIX = "T";
  protected static final String EVENT_TYPE = "Event";
  protected static final String ENTITY_TYPE = "Entity", STATIC_ENTITY_TYPE = "Static-Event";
  protected static final String TYPE_NEXT_EVENT = "next-event", TYPE_RESULT = "result", TYPE_AGENT = "agent", TYPE_ORIGIN = "origin",
      TYPE_COTEMPORAL_EVENT = "cotemporal", TYPE_SAME_EVENT = "same-event", TYPE_SUPER_EVENT = "super-event", TYPE_ENABLES = "enables",
      TYPE_DESTINATION = "destination", TYPE_LOCATION = "location", TYPE_THEME = "theme", TYPE_SAME_ENTITY = "same-entity",
      TYPE_TIME = "time", TYPE_RAW_MATERIAL = "raw-material", TYPE_CAUSE ="cause";
  
  public static int numTokens = 0, numSentences = 0, maxTokensPerProcess = 0, minTokensPerProcess = Integer.MAX_VALUE,
		  maxSentencesPerProcess = 0, minSentencesPerProcess = Integer.MAX_VALUE, numFilesRead = 0;
  
  //static int StaticEventCount =0;
 
  public final List<Example> parseFolder(String path) throws IOException {
    List<Example> examples = new ArrayList<Example>();
    File folder = new File(path);
    FilenameFilter textFilter = new FilenameFilter() {
      public boolean accept(File dir, String name) {
        String lowercaseName = name.toLowerCase();
        if (lowercaseName.endsWith(TEXT_EXTENSION)) {
          return true;
        } else {
          return false;
        }
      }
    };
    for(String file:folder.list(textFilter)){
      LogInfo.logs(file);
      
      String rawText = IOUtils.slurpFile(new File(path + file));
      Example example = new Example();
      example.data = rawText;
      example.id = file.replace(TEXT_EXTENSION, "");
      example.gold = createAnnotation(path + file);
      
      //Ensuring triadic closure
      /*
      List<EventMention> eventMentions = example.gold.get(EventMentionsAnnotation.class);
      for(int i = 0; i < eventMentions.size(); i++){
			for(int j = i+1; j < eventMentions.size(); j++){
				for(int k = j+1; k < eventMentions.size(); k++) {
					String rel1 = Utils.getEventEventRelation(example.gold, eventMentions.get(i).getTreeNode(), eventMentions.get(j).getTreeNode()).toString();
					String rel2 = Utils.getEventEventRelation(example.gold, eventMentions.get(j).getTreeNode(), eventMentions.get(k).getTreeNode()).toString();
					String rel3 = Utils.getEventEventRelation(example.gold, eventMentions.get(i).getTreeNode(), eventMentions.get(k).getTreeNode()).toString();
					Triple<String, String, String> goldEquivalent = Utils.getEquivalentBaseTriple(new Triple<String, String, String>(rel1, rel2, rel3));
					if((goldEquivalent.first().equals("NONE") && goldEquivalent.second().equals("SameEvent") && goldEquivalent.third().equals("SameEvent"))) {
						LogInfo.logs("NOTRIADCLOSURE" + example.id);
						if(rel1.equals("NONE")) {
							eventMentions.get(i).addArgument(eventMentions.get(j), RelationType.SameEvent);
						}
						else if(rel2.equals("NONE")) {
							eventMentions.get(j).addArgument(eventMentions.get(k), RelationType.SameEvent);
						}
						else if(rel3.equals("NONE")) {
							eventMentions.get(i).addArgument(eventMentions.get(k), RelationType.SameEvent);
						}
					}
				}
			}
      }*/
      
      example.prediction = example.gold.copy();
      example.prediction.set(EntityMentionsAnnotation.class, new ArrayList<EntityMention>());
      example.prediction.set(EventMentionsAnnotation.class, new ArrayList<EventMention>());
      examples.add(example);
      //break;
    }
    //LogInfo.logs("Number of static events - " + StaticEventCount);
    return examples;
  }
  
  public Example createAnnotationFromString(String input) {
	Annotation document = new Annotation(input);
    processor.annotate(document);
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);

    for(CoreMap sentence:sentences) {
    	Tree syntacticParse = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
    	List<EventMention> eventMentions = new ArrayList<EventMention>();
        sentence.set(EventMentionsAnnotation.class, eventMentions);
        
        List<EntityMention> entityMentions = new ArrayList<EntityMention>();
        sentence.set(EntityMentionsAnnotation.class, entityMentions);
    	syntacticParse.setSpans();
    }
    
    Example example = new Example();
    example.data = input;
    example.id = "Interactive";
    example.gold = document;
    
    example.prediction = document;
    example.prediction.set(EntityMentionsAnnotation.class, new ArrayList<EntityMention>());
    example.prediction.set(EventMentionsAnnotation.class, new ArrayList<EventMention>());
    return example;
  }
  
  private Annotation createAnnotation(String fileName) {
    String rawText = "";
    try {
      rawText = IOUtils.slurpFile(new File(fileName));
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }
    Annotation document = new Annotation(rawText);
    processor.annotate(document);
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    //Setting spans of the tree nodes of each sentence to avoid running into excpetions where we encounter sentences without any entities later.
    
    numFilesRead += 1;
    numSentences += sentences.size();
    if(sentences.size() > maxSentencesPerProcess) {
    	maxSentencesPerProcess = sentences.size();
    	LogInfo.logs("Updated maxSentencesPerProcess - " + fileName);
    }
    else if(sentences.size() < minSentencesPerProcess) {
    	minSentencesPerProcess = sentences.size();
    	LogInfo.logs("Updated minSentencesPerProcess - " + fileName);
    }
    
    int tokenCount = 0;
    for(CoreMap sentence:sentences) {
    	tokenCount += sentence.get(TokensAnnotation.class).size();
    	Tree syntacticParse = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
    	
    	List<EventMention> eventMentions = new ArrayList<EventMention>();
        sentence.set(EventMentionsAnnotation.class, eventMentions);
        
        List<EntityMention> entityMentions = new ArrayList<EntityMention>();
        sentence.set(EntityMentionsAnnotation.class, entityMentions);
        
    	syntacticParse.setSpans();
    }
    
    numTokens += tokenCount;
    
    if(tokenCount > maxTokensPerProcess) {
    	maxTokensPerProcess = tokenCount;
    	LogInfo.logs("Updated maxTokensPerProcess - " + fileName);
    }
    else if(tokenCount < minTokensPerProcess) {
    	minTokensPerProcess = tokenCount;
    	LogInfo.logs("Updated minTokensPerProcess - " + fileName);
    }
    
    HashMap<String, ArgumentMention> mentions = new HashMap<String, ArgumentMention>();
    try {
      RandomAccessFile reader = new RandomAccessFile(new File(fileName.replace(TEXT_EXTENSION, ANNOTATION_EXTENSION)), "r");
      String line;
      while((line = reader.readLine())!=null) {
    	LogInfo.logs(line);
        String[] splits = line.split("\t");
        String desc = splits[0];
        switch(desc.charAt(0)) {
          case 'T':
            String[] argumentDetails = splits[1].split(" ");
            String type = argumentDetails[0];
            ArgumentMention m;
            
            int begin = Integer.parseInt(argumentDetails[1]), end =  Integer.parseInt(argumentDetails[2]);
            CoreMap sentence = Utils.getContainingSentence(sentences, begin, end);
            Span span = Utils.getSpanFromSentence(sentence, begin, end);
            
            if(type.equals(EVENT_TYPE) || type.equals(STATIC_ENTITY_TYPE)) {
              m = new EventMention(desc, sentence, span);
              //LogInfo.logs("\t\t\t\t" + line);
              Tree eventRoot = Utils.getEventNode(sentence, (EventMention)m);
              IndexedWord head = Utils.findDependencyNode(sentence, eventRoot);
              //Utils.findDepthInDependencyTree(sentence, eventRoot);
              m.setHeadInDependencyTree(head);
              m.setTreeNode(eventRoot);
              mentions.put(desc, m);
            }
            else {
              //LogInfo.logs(line);
              m = new EntityMention(desc, sentence, span);
              Tree entityRoot = Utils.getEntityNode(sentence, (EntityMention)m);
              m.setTreeNode(entityRoot);
              Utils.addAnnotation(document, (EntityMention)m);
              IndexedWord head = Utils.findDependencyNode(sentence, entityRoot);
              m.setHeadInDependencyTree(head);
              m.setHeadTokenSpan(Utils.findEntityHeadWord((EntityMention)m));
              //LogInfo.logs(m.getHeadToken().originalText());
              if (!m.getHead().equals(new Span()))
                mentions.put(desc, m);
            }
            break;
          case 'E':
        	String[] parameters = splits[1].split(" ");
        	String[] splts = parameters[0].split(":");
        		
   			((EventMention)mentions.get(splts[1])).eventType = splts[0].equals(EVENT_TYPE) ? EventType.Event : EventType.StaticEvent;
   			//if(splts[0].equals(STATIC_ENTITY_TYPE))
   			//	StaticEventCount += 1;
    		mentions.put(desc, mentions.get(splts[1]));
    		mentions.remove(splts[1]);
         } 
      }
      reader.seek(0);
      while((line = reader.readLine())!=null) {
        String[] splits = line.split("\t");
        String desc = splits[0];
        switch(desc.charAt(0)) {
          case 'E':
            String[] parameters = splits[1].split(" ");
            EventMention event = (EventMention)mentions.get(desc);
            for(String parameter:parameters) {
              String[] keyValue = parameter.split(":");
              //System.out.println(keyValue[0] + "-" + keyValue[1]);
              if(keyValue[0].startsWith(TYPE_AGENT))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Agent);
              if(keyValue[0].startsWith(TYPE_ORIGIN))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Origin);
              if(keyValue[0].startsWith(TYPE_DESTINATION))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Destination);
              if(keyValue[0].startsWith(TYPE_LOCATION))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Location);
              if(keyValue[0].startsWith(TYPE_RESULT))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Result);
              if(keyValue[0].startsWith(TYPE_RAW_MATERIAL))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.RawMaterial);
              if(keyValue[0].startsWith(TYPE_THEME))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Theme);
              if(keyValue[0].startsWith(TYPE_TIME))
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Time);
              if(keyValue[0].startsWith(TYPE_COTEMPORAL_EVENT)) {
            		event.addArgument(mentions.get(keyValue[1]), RelationType.CotemporalEvent);
                    //((EventMention)mentions.get(keyValue[1])).addArgument(event, RelationType.CotemporalEvent);
              }
              if(keyValue[0].startsWith(TYPE_NEXT_EVENT)) {
                    event.addArgument(mentions.get(keyValue[1]), RelationType.NextEvent);
              }
              if(keyValue[0].startsWith(TYPE_SAME_EVENT)) {
                    event.addArgument(mentions.get(keyValue[1]), RelationType.SameEvent);
              }
              if(keyValue[0].startsWith(TYPE_CAUSE)){
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Causes); 
              }
              if(keyValue[0].startsWith(TYPE_SUPER_EVENT)) {
                    event.addArgument(mentions.get(keyValue[1]), RelationType.SuperEvent);
              }
              if(keyValue[0].startsWith(TYPE_ENABLES)) {
                    event.addArgument(mentions.get(keyValue[1]), RelationType.Enables);
              }
            }
            Utils.addAnnotation(document, event);
            break;
          case '*':
        	  String[] params = splits[1].split(" ");
        	  
        	  if(params[0].equals(TYPE_SAME_ENTITY)) {
        		  String entity1 = params[1], entity2 = params[2];
        		  ((EntityMention)mentions.get(entity1)).addRelation((EntityMention)mentions.get(entity2), RelationType.SameEntity);
        		  ((EntityMention)mentions.get(entity2)).addRelation((EntityMention)mentions.get(entity1), RelationType.SameEntity);
        	  }
        }
      }
      reader.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return document;
  }
}
