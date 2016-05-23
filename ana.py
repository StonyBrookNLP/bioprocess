from __future__ import division
from __future__ import print_function
import os
import pickle
from os.path import join
import sys
from glob import iglob
import itertools

# peterbe.com
def f7(seq):
    seen = set()
    seen_add = seen.add
    return [x for x in seq if not (x in seen or seen_add(x))]

wd = '/home/dick/srl/goto'
os.chdir(wd)
#exid = set([os.path.basename(os.path.splitext(x)[0]) for x in iglob(os.path.join(wd, 'lib/Dataset/train/*.ann'))])
# cldata = pickle.load(open(os.path.join(wd, 'lib/Dataset/lin.p'), "r"))
#exid - set(f7([x['exampleID'] for x in argdata]))

roles = ['Agent','Origin','Destination','Location','Result','RawMaterial','Theme','Time']
nsamples = { r:len([x for x in argdata if x['role'] == r]) for r in roles }
sroles = sorted(roles, reverse=True, key=lambda r:nsamples[r])
for r in itertools.chain(['lin']):
    with open(os.path.join(wd, 'lib/Dataset', r + '.p'), 'r') as fp:
        argdata = pickle.load(fp)
    for howmany in xrange(1,len(sroles)+1):
        fp = 0
        fn = 0
        tp = 0
        for role in sroles[0:howmany]:
            fn = fn + len([x for x in argdata if x['role'] == role and x['guessRole'] != x['role']])
            tp = tp + len([x for x in argdata if x['role'] == role and x['guessRole'] == x['role']])
            fp = fp + len([x for x in argdata if x['role'] != role and x['guessRole'] == role])
            if (tp+fp == 0):
                fp = 1
        frac = sum(nsamples[x] for x in sroles[0:howmany])/sum(nsamples.values())
        p = tp/(tp+fp)
        r= tp/(tp+fn)
        f1= 2*p*r/(p+r)
        print("{0} & {1:.2f} & {2:.2f} & {3:.2f} & {4:.2f}".format(','.join(sroles[0:howmany]), frac, p,r,f1 ))

for role in itertools.chain(['log']):
    with open(os.path.join(wd, 'lib/Dataset', role + '.p'), 'r') as fp:
        argdata = pickle.load(fp)
    fn = len([x for x in argdata if x['role'] != 'NONE' and x['guessRole'] == 'NONE'])
    tp = len([x for x in argdata if x['role'] != 'NONE' and x['guessRole'] != 'NONE'])
    fp = len([x for x in argdata if x['role'] == 'NONE' and x['guessRole'] != 'NONE'])
    if tp+fp == 0:
        fp = 1
    p = tp/(tp+fp)
    r= tp/(tp+fn)
    f1= 2*p*r/(p+r)
    print('{0} & {1:.2f} & {2:.2f} & {3:.2f}'.format(role, p,r,f1 ))

for role in sroles:
    argdata = pickle.load(open(os.path.join(wd, 'lib/Dataset', role + '.p'), "r"))
    fn = len([x for x in argdata if x['role'] == role and x['guessRole'] != role])
    tp = len([x for x in argdata if x['role'] == role and x['guessRole'] == role])
    fp = len([x for x in argdata if x['role'] == 'NONE' and x['guessRole'] == role])
    if tp+fp == 0:
        fp = 1
    p = tp/(tp+fp)
    r= tp/(tp+fn)
    if p+r == 0:
        p = 1
    f1= 2*p*r/(p+r)
    print('{0} & {1:.2f} & {2:.2f} & {3:.2f}'.format(role, p,r,f1 ))

