package edu.stanford.nlp.bioprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.Math;

import fig.basic.LogInfo;
/**
 * Split training file to cross validation splits given the total number of folds and the nth fold of test required
 * 
 * @author Rose
 */
public class CrossValidationSplit  {
	ArrayList<Example> allExamplesCV;
	int numFoldsCV;
	List<ArrayList<Example>> foldsCV;
	List<Integer> randomExampleIndex = new ArrayList<Integer>();
	
	public CrossValidationSplit(List<Example> list, int numFolds){
		randomExampleIndex = Arrays.asList(new Integer[]{209, 234, 135, 18, 161, 221, 240, 142, 170, 193, 180, 14, 102, 65, 12, 104, 173, 163, 177, 39, 37, 46, 218, 90, 252, 110, 156, 244, 260, 202, 100, 116, 74, 128, 50, 109, 56, 141, 247, 45, 66, 256, 129, 237, 130, 175, 97, 23, 120, 150, 91, 124, 139, 203, 254, 155, 55, 257, 147, 106, 144, 235, 238, 85, 83, 242, 253, 80, 20, 194, 78, 179, 72, 92, 192, 231, 43, 7, 111, 96, 77, 6, 49, 42, 59, 117, 149, 138, 153, 22, 105, 239, 167, 243, 134, 185, 64, 118, 190, 95, 26, 52, 57, 33, 145, 187, 216, 248, 228, 2, 53, 38, 199, 160, 68, 259, 10, 215, 126, 24, 154, 137, 222, 71, 191, 67, 35, 251, 82, 28, 9, 159, 195, 246, 158, 73, 204, 44, 146, 13, 11, 69, 219, 51, 29, 169, 21, 220, 123, 162, 151, 31, 232, 47, 196, 103, 181, 36, 54, 230, 225, 125, 25, 115, 261, 70, 255, 172, 89, 119, 0, 76, 98, 87, 132, 166, 113, 48, 1, 62, 133, 164, 157, 249, 61, 152, 207, 8, 174, 188, 112, 136, 88, 227, 114, 63, 5, 131, 58, 40, 171, 206, 81, 122, 3, 205, 127, 41, 210, 245, 212, 4, 250, 75, 168, 79, 165, 211, 189, 178, 184, 108, 208, 60, 94, 233, 99, 143, 140, 27, 107, 32, 183, 84, 176, 86, 236, 30, 93, 200, 226, 214, 34, 258, 101, 197, 201, 121, 241, 182, 148, 217, 229, 213, 224, 223, 198, 16, 17, 15, 186, 19});
		if (list.size() != randomExampleIndex.size()) {
			List<Integer> replacement = new ArrayList<Integer>();
			for (int i = 0; i < list.size(); i++)
				replacement.add(i);
			Collections.shuffle(replacement);
			randomExampleIndex = replacement;
		}
		LogInfo.logs(randomExampleIndex);
		numFoldsCV = numFolds;
		allExamplesCV = (ArrayList<Example>)list;
		foldsCV = new ArrayList<ArrayList<Example>>();
		int numSamples = (int) Math.floor(list.size()/numFolds);
		for (int i = 0 ; i < numFolds ; i++){
			ArrayList<Example> devCV = new ArrayList<Example>();
			for (int j = 0 ; j < numSamples ; j++){
				int elemNum = (i * numSamples + j);
				devCV.add(list.get(randomExampleIndex.get(elemNum)));
			}
			foldsCV.add(devCV);
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<Example> GetTrainExamples(int numFold){
		ArrayList<Example> trainCV = new ArrayList<Example>();
		if (numFold<1 || numFold>numFoldsCV){
			return trainCV;
		}
		trainCV = (ArrayList<Example>)allExamplesCV.clone();
 		trainCV.removeAll(foldsCV.get(numFold-1));
		return trainCV;
	}
	public List<Example> GetTestExamples(int numFold){
		List<Example> testCV = new ArrayList<Example>();
		if (numFold<1 || numFold>numFoldsCV){
			return testCV;
		}
		return foldsCV.get(numFold-1);
	}

}

