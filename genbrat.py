from __future__ import division
from __future__ import print_function
import pickle
import random
import re
from shutil import copyfile,move,rmtree
from os import listdir
from pickle import load
from nltk.parse.stanford import StanfordParser
import operator
from glob import iglob
import os
import re
import nltk.data
from nltk.tokenize import *
from nltk.internals import deprecated

from nltk import Tree
from nltk.corpus.reader.util import *
from nltk.corpus.reader import PlaintextCorpusReader
from nltk.corpus.reader.api import *
import sys
import collections

wd='/home/dick/srl/goto'
os.environ['CLASSPATH'] = os.path.join(wd, '../lib')
bratdir = os.path.join(wd, 'lib/Dataset/train.control')
rmtree(os.path.join("/tmp", "train"), ignore_errors=True)
if os.path.exists(os.path.join(wd, 'lib/Dataset/train')):
    move(os.path.join(wd, 'lib/Dataset/train'), os.path.join("/tmp", "train"))
os.mkdir(os.path.join(wd, 'lib/Dataset/train'))


global NPS
global SENT
def nps(t, offs):
    while SENT[offs] == ' ':
        offs = offs + 1
    boffs = offs
    orig = len(NPS)
    for child in t:
        if isinstance(child, Tree):
            offs = nps(child, offs)
        else:
            child = re.sub(r'-LRB-', '(', child)
            child = re.sub(r'-RRB-', ')', child)
            child = re.sub(r'``', '"', child)
            child = re.sub(r"''", '"', child)
            offs = SENT.index(child, offs) + len(child)
    # len(NPS) == orig means no subtrees had NP, i.e., i am the
    # most granular NP at this recursion depth
    qPossessiveNP = len(t) == 2 and t[0].label() == 'NNP' and t[1].label() == 'POS'
    if (len(NPS) == orig and t.label() == 'NP' and not qPossessiveNP):
        NPS.append((boffs, offs))
    return offs

def overlap(a, b):
    return (a[0] >= b[0] and a[0] < b[1] 
            or b[0] >= a[0] and b[0] < a[1])
    
parser = StanfordParser(os.path.join(wd, '../lib/stanford-corenlp-3.6.0.jar'),
                        os.path.join(wd, '../lib/stanford-english-corenlp-2016-01-10-models.jar'),
                        verbose=True)

# np = np(tree, 0)
# sys.exit()

for txt in iglob(os.path.join(bratdir, '*.txt')):
    which = os.path.basename(os.path.splitext(txt)[0])
    annfile = os.path.join(bratdir, which + '.ann')
    ann = {}
    with open(os.path.splitext(txt)[0] + '.ann') as fh:
        for line in fh.readlines():
            words = line.split()
            ann[words[0]] = words[1:]
    events = {k: v for k,v in ann.iteritems() if k[0] == 'E'}
    textbound = {k: v for k,v in ann.iteritems() if k[0] == 'T'}

    offs = 0
    new_args_for = collections.defaultdict(list)

    reader = PlaintextCorpusReader(bratdir, [os.path.basename(txt)])
    text = reader.raw()
    text = text.rstrip('\n')
    SENT = ""
    for sent in reader._sent_tokenizer.tokenize(text):
        sent_events = {}
        sent_assigned = {}
        for k,v in events.iteritems():
            TEv = v[0].split(':')[1]
            istart = int(ann[TEv][1])
            iend = int(ann[TEv][2])
            if overlap((istart, iend), (offs, offs+len(sent))):
                sent_events[istart] = k
            for TArg in {item.split(':')[1] for item in v[1:]}:
                if TArg[0] == 'T':
                    sent_assigned[TArg] = (int(ann[TArg][1]),int(ann[TArg][2]))
        
        tree = list(parser.raw_parse(sent)).pop()
        NPS = []
        SENT = text[0:text.index(sent, offs) + len(sent)]
        offs = nps(tree, offs)
        new_args = []
        for np in NPS:
            overlaps = [x for x in sent_assigned.values() if overlap(np, x)]
            if len(overlaps) == 0:
                new_args.append(np)
        for np in new_args:
            shortest = (len(sent), '')
            for istart,TEv in sent_events.iteritems():
                s = min(np[0], istart)
                e = max(np[0], istart)
                dist = text[s:e].count(' ')
                if dist < shortest[0]:
                    shortest = (dist, TEv)
            new_args_for[shortest[1]].append(np)

    copyfile(txt, os.path.join(wd, 'lib/Dataset/train', os.path.basename(txt)))
    annfile2 = os.path.join(wd, 'lib/Dataset/train', which + '.ann')
    Tnext = max([int(x[1:]) for x in textbound])
    with open(annfile, 'r') as a, open(annfile2, 'w') as b:
        newTs = {}
        for line in [line.rstrip('\n') for line in a]:
            words = line.split()
            if new_args_for.has_key(words[0]):
                for np in new_args_for[words[0]]:
                    Tnext = Tnext + 1
                    newTs['T' + str(Tnext)] = np
                    line = line + ' none:T' + str(Tnext)
            print(line, file=b)
        for t, np in sorted(newTs.iteritems()):
            print("%s\tEntity %d %d\t%s" % (t, np[0], np[1], text[np[0]:np[1]]), file=b)
            
    # for k,v in events.iteritems():
    #     TEv = v[0].split(':')[1]
    #     TGold = set([item.split(':')[1] for item in v[1:]])
    #     istart = int(ann[TEv][1])
    #     iend = istart + len(reader._sent_tokenizer.tokenize(text[istart:]).pop(0))
    #     sent = reader._sent_tokenizer.tokenize(text[:iend]).pop()
    #     tree = list(parser.parse(sent)).pop()

