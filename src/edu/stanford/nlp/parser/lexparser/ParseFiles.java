package edu.stanford.nlp.parser.lexparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.parser.metrics.AbstractEval;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.DocumentPreprocessor.DocType;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.Function;
import edu.stanford.nlp.util.ScoredObject;
import edu.stanford.nlp.util.Timing;
import edu.stanford.nlp.util.concurrent.MulticoreWrapper;



/**
 * Runs the parser over a set of files.  This is useful for making it
 * operate in a multithreaded manner.  If you want access to the
 * various stats it keeps, create the object and call parseFiles;
 * otherwise, the static parseFiles is a good convenience method.
 *
 * @author John Bauer (refactored from existing code)
 */
public class ParseFiles {
  final TreebankLanguagePack tlp;
  // todo: perhaps the output streams could be passed in
  final PrintWriter pwOut;
  final PrintWriter pwErr;

  int numWords = 0;
  int numSents = 0;
  int numUnparsable = 0;
  int numNoMemory = 0;
  int numFallback = 0;
  int numSkipped = 0;

  boolean saidMemMessage = false;

  final boolean runningAverages;
  final boolean summary;

  final AbstractEval.ScoreEval pcfgLL;
  final AbstractEval.ScoreEval depLL;
  final AbstractEval.ScoreEval factLL;

  final Options op;

  final ParserQueryFactory pqFactory;

  final TreePrint treePrint;

  /** Parse the files with names given in the String array args elements from
   *  index argIndex on.  Convenience method which builds and invokes a ParseFiles object.  
   */
  static void parseFiles(String[] args, int argIndex, boolean tokenized, TokenizerFactory<? extends HasWord> tokenizerFactory, String elementDelimiter, String sentenceDelimiter, Function<List<HasWord>, List<HasWord>> escaper, String tagDelimiter, Options op, TreePrint treePrint, ParserQueryFactory pqFactory) {
    ParseFiles pf = new ParseFiles(op, treePrint, pqFactory);
    pf.parseFiles(args, argIndex, tokenized, tokenizerFactory, elementDelimiter, sentenceDelimiter, escaper, tagDelimiter);
  }

  public ParseFiles(Options op, TreePrint treePrint, ParserQueryFactory pqFactory) {
    this.op = op;
    this.pqFactory = pqFactory;
    this.treePrint = treePrint;

    this.tlp = op.tlpParams.treebankLanguagePack();
    this.pwOut = op.tlpParams.pw();
    this.pwErr = op.tlpParams.pw(System.err);

    if (op.testOptions.verbose) {
      pwErr.println("Sentence final words are: " + Arrays.asList(tlp.sentenceFinalPunctuationWords()));
      pwErr.println("File encoding is: " + op.tlpParams.getInputEncoding());
    }

    // evaluation setup
    this.runningAverages = Boolean.parseBoolean(op.testOptions.evals.getProperty("runningAverages"));
    this.summary = Boolean.parseBoolean(op.testOptions.evals.getProperty("summary"));
    if (Boolean.parseBoolean(op.testOptions.evals.getProperty("pcfgLL"))) {
      this.pcfgLL = new AbstractEval.ScoreEval("pcfgLL", runningAverages);
    } else {
      this.pcfgLL = null;
    }
    if (Boolean.parseBoolean(op.testOptions.evals.getProperty("depLL"))) {
      this.depLL = new AbstractEval.ScoreEval("depLL", runningAverages);
    } else {
      this.depLL = null;
    }
    if (Boolean.parseBoolean(op.testOptions.evals.getProperty("factLL"))) {
      this.factLL = new AbstractEval.ScoreEval("factLL", runningAverages);
    } else {
      this.factLL = null;
    }

  }

  public void parseFiles(String[] args, int argIndex, boolean tokenized, TokenizerFactory<? extends HasWord> tokenizerFactory, String elementDelimiter, String sentenceDelimiter, Function<List<HasWord>, List<HasWord>> escaper, String tagDelimiter) {
    final DocType docType = (elementDelimiter == null) ? DocType.Plain : DocType.XML;

    if (op.testOptions.verbose) {
      if(tokenizerFactory != null)
        pwErr.println("parseFiles: Tokenizer factory is: " + tokenizerFactory);
    }

    final Timing timer = new Timing();
    timer.start();

    //Loop over the files
    for (int i = argIndex; i < args.length; i++) {
      final String filename = args[i];

      final DocumentPreprocessor documentPreprocessor;
      if (filename.equals("-")) {
        try {
          documentPreprocessor = new DocumentPreprocessor(new BufferedReader(new InputStreamReader(System.in, op.tlpParams.getInputEncoding())),docType);
        } catch (IOException e) {
          throw new RuntimeIOException(e);
        }
      } else {
        documentPreprocessor = new DocumentPreprocessor(filename,docType,op.tlpParams.getInputEncoding());
      }

      //Unused values are null per the main() method invocation below
      //null is the default for these properties
      documentPreprocessor.setSentenceFinalPuncWords(tlp.sentenceFinalPunctuationWords());
      documentPreprocessor.setEscaper(escaper);
      documentPreprocessor.setSentenceDelimiter(sentenceDelimiter);
      documentPreprocessor.setTagDelimiter(tagDelimiter);
      documentPreprocessor.setElementDelimiter(elementDelimiter);
      if(tokenizerFactory == null)
        documentPreprocessor.setTokenizerFactory((tokenized) ? null : tlp.getTokenizerFactory());
      else
        documentPreprocessor.setTokenizerFactory(tokenizerFactory);

      //Setup the output
      PrintWriter pwo = pwOut;
      if (op.testOptions.writeOutputFiles) {
        String normalizedName = filename;
        try {
          URL url = new URL(normalizedName); // this will exception if not a URL
          normalizedName = normalizedName.replaceAll("/","_");
        } catch (MalformedURLException e) {
          //It isn't a URL, so silently ignore
        }

        String ext = (op.testOptions.outputFilesExtension == null) ? "stp" : op.testOptions.outputFilesExtension;
        String fname = normalizedName + '.' + ext;
        if (op.testOptions.outputFilesDirectory != null && !op.testOptions.outputFilesDirectory.equals("")) {
          String fseparator = System.getProperty("file.separator");
          if (fseparator == null || "".equals(fseparator)) {
            fseparator = "/";
          }
          File fnameFile = new File(fname);
          fname = op.testOptions.outputFilesDirectory + fseparator + fnameFile.getName();
        }

        try {
          pwo = op.tlpParams.pw(new FileOutputStream(fname));
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
      }
      treePrint.printHeader(pwo, op.tlpParams.getOutputEncoding());


      pwErr.println("Parsing file: " + filename);
      int num = 0;
      int numProcessed = 0;
      if (op.testOptions.testingThreads != 1) {
        MulticoreWrapper<List<? extends HasWord>, ParserQuery> wrapper = new MulticoreWrapper<List<? extends HasWord>, ParserQuery>(op.testOptions.testingThreads, new ParsingThreadsafeProcessor(pqFactory, pwErr));

        for (List<HasWord> sentence : documentPreprocessor) {
          num++;
          numSents++;
          int len = sentence.size();
          numWords += len;
          pwErr.println("Parsing [sent. " + num + " len. " + len + "]: " + Sentence.listToString(sentence, true));

          wrapper.put(sentence);
          while (wrapper.peek()) {
            ParserQuery pq = wrapper.poll();
            processResults(pq, numProcessed++, pwo);
          }
        }

        wrapper.join();
        while (wrapper.peek()) {
          ParserQuery pq = wrapper.poll();
          processResults(pq, numProcessed++, pwo);
        }
      } else {
        ParserQuery pq = pqFactory.parserQuery();
        for (List<HasWord> sentence : documentPreprocessor) {
          num++;
          numSents++;
          int len = sentence.size();
          numWords += len;
          pwErr.println("Parsing [sent. " + num + " len. " + len + "]: " + Sentence.listToString(sentence, true));
          pq.parseAndReport(sentence, pwErr);
          processResults(pq, numProcessed++, pwo);
        }
      }

      treePrint.printFooter(pwo);
      if (op.testOptions.writeOutputFiles) pwo.close();

      pwErr.println("Parsed file: " + filename + " [" + num + " sentences].");
    }

    long millis = timer.stop();

    if (summary) {
      if (pcfgLL != null) pcfgLL.display(false, pwErr);
      if (depLL != null) depLL.display(false, pwErr);
      if (factLL != null) factLL.display(false, pwErr);
    }

    if (saidMemMessage) {
      ParserUtils.printOutOfMemory(pwErr);
    }
    double wordspersec = numWords / (((double) millis) / 1000);
    double sentspersec = numSents / (((double) millis) / 1000);
    NumberFormat nf = new DecimalFormat("0.00"); // easier way!
    pwErr.println("Parsed " + numWords + " words in " + numSents +
        " sentences (" + nf.format(wordspersec) + " wds/sec; " +
        nf.format(sentspersec) + " sents/sec).");
    if (numFallback > 0) {
      pwErr.println("  " + numFallback + " sentences were parsed by fallback to PCFG.");
    }
    if (numUnparsable > 0 || numNoMemory > 0 || numSkipped > 0) {
      pwErr.println("  " + (numUnparsable + numNoMemory + numSkipped) + " sentences were not parsed:");
      if (numUnparsable > 0) {
        pwErr.println("    " + numUnparsable + " were not parsable with non-zero probability.");
      }
      if (numNoMemory > 0) {
        pwErr.println("    " + numNoMemory + " were skipped because of insufficient memory.");
      }
      if (numSkipped > 0) {
        pwErr.println("    " + numSkipped + " were skipped as length 0 or greater than " + op.testOptions.maxLength);
      }
    }
  } // end parseFiles

  public void processResults(ParserQuery parserQuery, int num, PrintWriter pwo) {
    if (parserQuery.parseSkipped()) {
      List<? extends HasWord> sentence = parserQuery.originalSentence();
      if (sentence != null) {
        numWords -= sentence.size();
      }
      numSkipped++;
    }
    if (parserQuery.parseNoMemory()) numNoMemory++;
    if (parserQuery.parseUnparsable()) numUnparsable++;
    if (parserQuery.parseFallback()) numFallback++;
    saidMemMessage = saidMemMessage || parserQuery.saidMemMessage();
    Tree ansTree = parserQuery.getBestParse();
    if (ansTree == null) {
      pwo.println("(())");
      return;
    }
    if (ansTree != null) {
      if (pcfgLL != null && parserQuery.getPCFGParser() != null) {
        pcfgLL.recordScore(parserQuery.getPCFGParser(), pwErr);
      }
      if (depLL != null && parserQuery.getDependencyParser() != null) {
        depLL.recordScore(parserQuery.getDependencyParser(), pwErr);
      }
      if (factLL != null && parserQuery.getFactoredParser() != null) {
        factLL.recordScore(parserQuery.getFactoredParser(), pwErr);
      }
    }
    try {
      treePrint.printTree(ansTree, Integer.toString(num), pwo);
    } catch (RuntimeException re) {
      pwErr.println("TreePrint.printTree skipped: out of memory (or other error)");
      re.printStackTrace();
      numNoMemory++;
      try {
        treePrint.printTree(null, Integer.toString(num), pwo);
      } catch (Exception e) {
        pwErr.println("Sentence skipped: out of memory or error calling TreePrint.");
        pwo.println("(())");
        e.printStackTrace();
      }
    }
    // crude addition of k-best tree printing
    // TODO: interface with the RerankingParserQuery
    if (op.testOptions.printPCFGkBest > 0 && parserQuery.getPCFGParser() != null && parserQuery.getPCFGParser().hasParse()) {
      List<ScoredObject<Tree>> trees = parserQuery.getKBestPCFGParses(op.testOptions.printPCFGkBest);
      treePrint.printTrees(trees, Integer.toString(num), pwo);
    } else if (op.testOptions.printFactoredKGood > 0 && parserQuery.getFactoredParser() != null && parserQuery.getFactoredParser().hasParse()) {
      // DZ: debug n best trees
      List<ScoredObject<Tree>> trees = parserQuery.getKGoodFactoredParses(op.testOptions.printFactoredKGood);
      treePrint.printTrees(trees, Integer.toString(num), pwo);
    }
  }
}
