package edu.stanford.nlp.bioprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Index;
import edu.stanford.nlp.util.Pair;

public class DynamicProgrammingSRL {
	Tree syntacticParse;
	IdentityHashMap<Tree, List<Pair<String, Double>>> tokenMap;
	IdentityHashMap<Tree, BioDatum> nodeDatumMap;
	IdentityHashMap<Tree, List<Pair<IdentityHashMap<Tree, String>, Double>>> nodeRanks;
	Index labelIndex;
	
	public DynamicProgrammingSRL(CoreMap sentence, IdentityHashMap<Tree, List<Pair<String, Double>>> map, List<BioDatum> data, Index labelIndex) {
		this.syntacticParse = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
		
		this.tokenMap = map;
		nodeDatumMap = new IdentityHashMap<Tree, BioDatum>();
		for (BioDatum d : data) {
			nodeDatumMap.put(d.entityNode, d);
		}

//		for (Tree t : this.syntacticParse.postOrderNodeList()) {
//			if (t.isLeaf()) continue;
//			//System.out.println("Looking for: "+t.pennString());
//			//System.out.println(t.getLeaves()+":"+nodeDatumMap.get(t).getBestRole());
//		}
		//System.out.println("Syntactic parse tree: ");
		//this.syntacticParse.pennPrint();
		this.nodeRanks = new IdentityHashMap<Tree, List<Pair<IdentityHashMap<Tree, String>, Double>>> ();
		this.labelIndex = labelIndex;
		for (Tree t : this.syntacticParse.postOrderNodeList()) {
//			if (!t.isLeaf() && !t.isPreTerminal() && !t.value().equals("ROOT")) {
			if (t.isPrePreTerminal()) {
				//System.out.println("--------------------"+t.toString()+"----------------------------");
				IdentityHashMap<Tree, String> allottedRoles = new IdentityHashMap<Tree, String>();
				List<Pair<IdentityHashMap<Tree, String>, Double>> withParentallPerms = new ArrayList<Pair<IdentityHashMap<Tree, String>, Double>>();
				List<Pair<IdentityHashMap<Tree, String>, Double>> allPerms = new ArrayList<Pair<IdentityHashMap<Tree, String>, Double>>();
				genPermutations(t.getChildrenAsList(), 0, allottedRoles, allPerms);
				for (int i=0; i<allPerms.size(); i++) {
					Pair<IdentityHashMap<Tree, String>, Double> permPair = allPerms.get(i);
					boolean allNone = true;
					for (Tree child : permPair.first.keySet()) {
						if (!permPair.first.get(child).equals("NONE")) {
							allNone = false;
						}
						//System.out.print(child.toString()+":"+permPair.first.get(child)+"---->");
					}
//					System.out.println("Prob: "+permPair.second);
					if (allNone) {
//						System.out.println("THIS IS ALL NONE");
						double withParentProb = permPair.second;
						String parentRole = null;
						if (nodeDatumMap.get(t) == null) {
							System.out.println("Empty nodeDatumMap");
						}
						for (int cntr=0; cntr < nodeDatumMap.get(t).rankedRoleProbs.size(); cntr++) {
//							System.out.println(permPair.second+" -mult by- "+nodeDatumMap.get(t).rankedRoleProbs.get(cntr).second);
							withParentProb = permPair.second * nodeDatumMap.get(t).rankedRoleProbs.get(cntr).second;
							parentRole = nodeDatumMap.get(t).rankedRoleProbs.get(cntr).first;
							permPair.first.put(t, parentRole);
							//permPair.second = withParentProb;
							//System.out.println("Parent: "+parentRole+":"+nodeDatumMap.get(t).rankedRoleProbs.get(cntr).second);
                            if (withParentProb > 1e-10) {
                                Pair<IdentityHashMap<Tree, String>, Double> permElem = new Pair<IdentityHashMap<Tree, String>, Double>();
                                permElem.first = (IdentityHashMap<Tree, String>) permPair.first.clone();
                                permElem.second = withParentProb;
                                withParentallPerms.add(permElem);
                            }
                        }
						//for (int printer = 0; printer < withParentallPerms.size(); printer++) {
						//	for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
								// System.out.println(iter.getLeaves()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
							// }
							//System.out.println("Prob-"+parentRole+": "+withParentallPerms.get(printer).second);
						//}
					} else {
						double withParentProb = permPair.second;
						String parentRole = null;
						//System.out.println(permPair.second+" -mult by- "+nodeDatumMap.get(t).getRoleProb("NONE"));
						withParentProb = permPair.second * nodeDatumMap.get(t).getRoleProb("NONE");
						parentRole = "NONE";
						permPair.first.put(t, parentRole);
						permPair.second = withParentProb;
						//System.out.println("Parent as None: "+parentRole+":"+nodeDatumMap.get(t).getRoleProb("NONE"));
						Pair<IdentityHashMap<Tree, String>, Double> permElem = new Pair<IdentityHashMap<Tree, String>, Double>();
						permElem.first = (IdentityHashMap<Tree, String>) permPair.first.clone();
						permElem.second = permPair.second;
						//System.out.println("Writing product as : "+permElem.second);
						withParentallPerms.add(permElem);
//						for (int printer = 0; printer < withParentallPerms.size(); printer++) {
//							for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
//								//System.out.print(iter.getLeaves()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
//							}
//							//System.out.println("Prob-"+parentRole+": "+withParentallPerms.get(printer).second);
//						}
					}
				}
				Collections.sort(withParentallPerms, new PairComparatorByDoubleHashMap());

				for (int printer = 0; printer < withParentallPerms.size(); printer++) {
					for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
						//System.out.print(iter.toString()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
					}
					//System.out.println("Prob-parent: "+withParentallPerms.get(printer).second);
				}
				//System.out.println("--------------------"+t.toString()+"---------------------------");
				nodeRanks.put(t, withParentallPerms.subList(0, 2));
//				break;
			} else if (!t.isLeaf() && !t.isPreTerminal()  && !t.value().equals("ROOT")) {
				//Tree t is an internal node but not prepreterminal
				//System.out.println("--------------------"+t.toString()+"ACCUMULATED----------------------------");
				IdentityHashMap<Tree, String> allottedRoles = new IdentityHashMap<Tree, String>();
				List<Pair<IdentityHashMap<Tree, String>, Double>> withParentallPerms = new ArrayList<Pair<IdentityHashMap<Tree, String>, Double>>();
				List<Pair<IdentityHashMap<Tree, String>, Double>> allPerms = new ArrayList<Pair<IdentityHashMap<Tree, String>, Double>>();
				for (Tree tempChild : t.getChildrenAsList()) {
					List<Pair<IdentityHashMap<Tree, String>, Double>> ranks = this.nodeRanks.get(tempChild);
					//System.out.println(tempChild.toString());
					if (ranks!=null) {
						for (int cntr=0; cntr<ranks.size(); cntr++) {
							for (Tree iter : ranks.get(cntr).first.keySet()) {
								//System.out.print(iter.toString()+":"+ranks.get(cntr).first.get(iter)+"---->");
							}
							//System.out.println("Prob-parent: "+ranks.get(cntr).second);
						}
					} else {
						//System.out.println("Null ranks");
					}
				}
				genPermutationsInternal(t.getChildrenAsList(), 0, allottedRoles, allPerms, 1.0);
				for (int i=0; i<allPerms.size(); i++) {
					Pair<IdentityHashMap<Tree, String>, Double> permPair = allPerms.get(i);
					boolean allNone = true;
					for (Tree child : permPair.first.keySet()) {
						if (!permPair.first.get(child).equals("NONE")) {
							allNone = false;
						}
						//System.out.print(child.toString()+":"+permPair.first.get(child)+"---->");
					}
					//System.out.println("Prob: "+permPair.second);
					if (allNone) {
						//System.out.println("THIS IS ALL NONE");
						double withParentProb = permPair.second;
						String parentRole = null;
						if (nodeDatumMap.get(t) == null) {
							//System.out.println("Empty nodeDatumMap");
						}
						if (t == this.syntacticParse) {
							//System.out.println("Root t");
						}
						for (int cntr=0; cntr < nodeDatumMap.get(t).rankedRoleProbs.size(); cntr++) {
							withParentProb = permPair.second * nodeDatumMap.get(t).rankedRoleProbs.get(cntr).second;
							parentRole = nodeDatumMap.get(t).rankedRoleProbs.get(cntr).first;
							permPair.first.put(t, parentRole);
							//permPair.second = withParentProb;
							//System.out.println("Parent: "+parentRole+":"+nodeDatumMap.get(t).rankedRoleProbs.get(cntr).second);
							Pair<IdentityHashMap<Tree, String>, Double> permElem = new Pair<IdentityHashMap<Tree, String>, Double>();
							permElem.first = (IdentityHashMap<Tree, String>) permPair.first.clone();
							permElem.second = withParentProb;
							withParentallPerms.add(permElem);
						}
//						for (int printer = 0; printer < withParentallPerms.size(); printer++) {
//							for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
//								//System.out.print(iter.getLeaves()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
//							}
//							//System.out.println("Prob-"+parentRole+": "+withParentallPerms.get(printer).second);
//						}
					} else {
						double withParentProb = permPair.second;
						String parentRole = null;
						withParentProb = permPair.second * nodeDatumMap.get(t).getRoleProb("NONE");
						parentRole = "NONE";
						permPair.first.put(t, parentRole);
						permPair.second = withParentProb;
						//System.out.println(t.toString()+"Parent: "+parentRole+":"+nodeDatumMap.get(t).getRoleProb("NONE"));
                        if (permPair.second > 1e-10) {
                            Pair<IdentityHashMap<Tree, String>, Double> permElem = new Pair<IdentityHashMap<Tree, String>, Double>();
                            permElem.first = (IdentityHashMap<Tree, String>) permPair.first.clone();
                            permElem.second = permPair.second;
                            withParentallPerms.add(permElem);
                        }
//						for (int printer = 0; printer < withParentallPerms.size(); printer++) {
//							for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
//								//System.out.print(iter.getLeaves()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
//							}
//							//System.out.println("Prob-"+parentRole+": "+withParentallPerms.get(printer).second);
//						}
					}
				}
				Collections.sort(withParentallPerms, new PairComparatorByDoubleHashMap());

				for (int printer = 0; printer < withParentallPerms.size(); printer++) {
					for (Tree iter : withParentallPerms.get(printer).first.keySet()) {
						//System.out.print(iter.toString()+":"+withParentallPerms.get(printer).first.get(iter)+"---->");
					}
					//System.out.println("Prob-parent: "+withParentallPerms.get(printer).second);
				}
				//System.out.println("--------------------"+t.toString()+"---------------------------");
				nodeRanks.put(t, withParentallPerms.subList(0, 2));
				//break;
			}
		}
		System.out.println("Final assignments: ");
		Pair<IdentityHashMap<Tree, String>, Double> finalRanks = this.nodeRanks.get(this.syntacticParse.getChild(0)).get(0);
		for (Tree temp : finalRanks.first.keySet()) {
			//System.out.println(temp.toString()+":"+finalRanks.first.get(temp));
			this.nodeDatumMap.get(temp).guessRole = finalRanks.first.get(temp);
		}
	}
	
	// Pair<IdentityHashMap<Tree, String>, Double>
	public void genPermutations(List<Tree> children, int k, IdentityHashMap<Tree, String> allottedRoles, List<Pair<IdentityHashMap<Tree, String>, Double>> allPerms) {
		if ( k != children.size() ) {
			Tree child = children.get(k);
//			//System.out.println("Looking for: "+child.pennString());
			BioDatum childDatum = nodeDatumMap.get(child);
			for (int i=0; i<childDatum.rankedRoleProbs.size(); i++) {
				String role = childDatum.rankedRoleProbs.get(i).first;
				allottedRoles.put(child, role);
//				//System.out.println("Recursive k: "+k);
				genPermutations(children, ++k, allottedRoles, allPerms);
				k--;
			}
		} else {
			double prob = 1.0;
			for (Tree t : allottedRoles.keySet()) {
//				//System.out.println(t.getLeaves().toString()+":"+allottedRoles.get(t));
//				for (int i=0; i<nodeDatumMap.get(t).rankedRoleProbs.size(); i++) {
//					//System.out.println(nodeDatumMap.get(t).rankedRoleProbs.get(i).first+":"+nodeDatumMap.get(t).rankedRoleProbs.get(i).second);
//				}
				prob *= nodeDatumMap.get(t).getRoleProb(allottedRoles.get(t));
			}
//			//System.out.println("Prob: "+prob);
            if (prob > 1e-15)
                allPerms.add(new Pair<IdentityHashMap<Tree, String>, Double>((IdentityHashMap<Tree, String>) allottedRoles.clone(), prob));
		}
	}
	
	public void genPermutationsInternal(List<Tree> children, int k, IdentityHashMap<Tree, String> allottedRoles, List<Pair<IdentityHashMap<Tree, String>, Double>> allPerms, double prob) {
        if (prob < 1e-15)
            return;
		if ( k != children.size() ) {
			Tree child = children.get(k);
			//System.out.println("\nChild: "+child.toString());
			List<Pair<IdentityHashMap<Tree, String>, Double>> childPerms = this.nodeRanks.get(child);
			if (childPerms != null) {
				for (int i=0; i<childPerms.size(); i++) {
//					String role = childDatum.rankedRoleProbs.get(i).first;
					//System.out.println();
					//System.out.println("Allotted Roles");
					for (Tree temp : allottedRoles.keySet()) {
						//System.out.print(temp.toString()+":"+allottedRoles.get(temp)+"--");
					}
					//System.out.println("\nMerging Roles");
					for (Tree temp : childPerms.get(i).first.keySet()) {
						//System.out.print(temp.toString()+":"+childPerms.get(i).first.get(temp)+"--");
					}
					Utils.mergeMaps(allottedRoles, childPerms.get(i).first);
					////System.out.println("Recursive k: "+k);
					genPermutationsInternal(children, ++k, allottedRoles, allPerms, prob*childPerms.get(i).second);
					k--;
				}
			} else {
				Tree child1 = children.get(k);
	//				//System.out.println("Looking for: "+child.pennString());
				BioDatum childDatum = nodeDatumMap.get(child1);
				for (int i=0; i<childDatum.rankedRoleProbs.size(); i++) {
					String role = childDatum.rankedRoleProbs.get(i).first;
					allottedRoles.put(child1, role);
					////System.out.println("Recursive pre terminal k: "+k);
					genPermutationsInternal(children, ++k, allottedRoles, allPerms, prob*childDatum.getRoleProb(role));
					k--;
				}
			}
		} else {
//			double prob = 1.0;
//			for (Tree t : allottedRoles.keySet()) {
////				//System.out.println(t.getLeaves().toString()+":"+allottedRoles.get(t));
////				for (int i=0; i<nodeDatumMap.get(t).rankedRoleProbs.size(); i++) {
////					//System.out.println(nodeDatumMap.get(t).rankedRoleProbs.get(i).first+":"+nodeDatumMap.get(t).rankedRoleProbs.get(i).second);
////				}
//				prob *= nodeDatumMap.get(t).getRoleProb(allottedRoles.get(t));
//			}
//			//System.out.println("Prob: "+prob);
			//System.out.println("\nAdded : ");
			for (Tree printer : allottedRoles.keySet()) {
//				System.out.print(printer.toString()+":"+allottedRoles.get(printer)+"--");
			}
			allPerms.add(new Pair<IdentityHashMap<Tree, String>, Double>((IdentityHashMap<Tree, String>) allottedRoles.clone(), prob));
		}
	}
	
	// Assumes the map has probs for E.
	// Probs of all. ?
	// Iterate over all and make all labels.
	// Take maximum and assign labels
	// Prepare for layers above.
	public void calculateLabels() {
		for (Tree node : this.syntacticParse.postOrderNodeList()) {
			if (node.isLeaf() || node.value().equals("ROOT")) {
				continue;
			}
			
			if (node.isPreTerminal()) {
				List<Pair<String, Double>> targetNodePair = this.tokenMap.get(node);
			}
			
			
			/*
			 * Pair<double[], String> targetNodePair = this.tokenMap.get(node);
			
			Double nodeO = Math.log(1-targetNodePair.first);
			Double nodeE = Math.log(targetNodePair.first);
			for (Tree child : node.getChildrenAsList()) {
				if (child.isLeaf()) {
					continue;
				}
				Pair<Double, String> nodeVals = this.tokenMap.get(child);
				nodeE += (Math.log(1-nodeVals.first));
				if (nodeVals.second.equals("O")) {
					nodeO += Math.log((1-nodeVals.first));
				} else {
					nodeO += Math.log(nodeVals.first);
				}
			}
			nodeO = Math.exp(nodeO);
			nodeE = Math.exp(nodeE);
			double sum = nodeO + nodeE;
			nodeO = nodeO/sum;
			nodeE = nodeE/sum;
			
			if (nodeO > nodeE) {// && !allchildrenE(node)) {
				targetNodePair.setFirst(1-nodeO);
				nodeDatumMap.get(node).setProbability(1-nodeO);
				targetNodePair.setSecond("O");
				nodeDatumMap.get(node).guessLabel = "O";
			} else {
				//LogInfo.logs("\n\n-------------------------Predicted Entity: "+node+":" +node.getSpan());
				targetNodePair.setFirst(nodeE);
				nodeDatumMap.get(node).setProbability(nodeE);
				targetNodePair.setSecond("E");
				nodeDatumMap.get(node).guessLabel = "E";
				for (Tree child : node.preOrderNodeList()) {
					if (child.isLeaf() || child.equals(node)) {
						continue;
					}
					//LogInfo.logs("Resetting " + child + " to O");
					this.tokenMap.get(child).setSecond("O");
					nodeDatumMap.get(child).guessLabel = "O";
				}
				//for(String n:nodeDatumMap.keySet())
				//	LogInfo.logs(n + ":" + node.getSpan() +":"+ nodeDatumMap.get(n).guessLabel );
				//LogInfo.logs("============================================================\n\n");
			}
			*/
		}
		
		//HACK - remove all DT
		for (Tree node : this.syntacticParse.postOrderNodeList()) {
			if(node.value().equals("DT"))
				nodeDatumMap.get(node).guessLabel = "O";
		}
	}
}
