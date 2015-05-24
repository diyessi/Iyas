import numpy as np
import pandas as pd
import sys
import random

from sklearn.multiclass import OneVsRestClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite
from sklearn.metrics import f1_score
from sklearn.grid_search import GridSearchCV
from sklearn.metrics import accuracy_score

RANDOM_STATE = 123

THREE_CLASSES= False
BOOL_RBF = False

MAJORITY_CLASS='EQUAL'

"""
We implement the classifier for the dataset in which comments from the same
thread are paired together and the task consists of predicting whether t hey 
belong to the same class or not.
"""

def main():
	
	random.seed(RANDOM_STATE)
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<gold file> <prediction file>"				
		sys.exit()
	else:
		print "INPUT FILES: ", sys.argv[1], sys.argv[2]
		explore_threshold_classif(sys.argv[1], sys.argv[2])	
		#subtask_b(sys.argv[1], sys.argv[2])	

def explore_threshold_classif(gold, pred):
	
	gold_ids, cgold = read_data_gold(gold)
	predict_ids, predict_labels, predict_confs = read_data_pred(pred)

	min_confidence = min(predict_confs)
	max_confidence = max(predict_confs)

	print min_confidence, max_confidence
	
	
	print "thres\tF\tA\tpassed\tdidn't"
	for i in drange(min_confidence-0.1, max_confidence+0.1, 0.1):
		per_range_predictions = []
		thres_up=0
		thres_down=0		
		for cl, predict_conf in zip(predict_labels, predict_confs):
			if (predict_conf < i):
				per_range_predictions.append(cl)
				thres_down+=1
			else:				
				 per_range_predictions.append(MAJORITY_CLASS)
				 thres_up+=1
		
		if THREE_CLASSES:
			f, acc = evaluate3classes(cgold, per_range_predictions)
		else:
			f, acc = evaluate2classes(cgold, per_range_predictions, pos_label="EQUAL")
		#print "thres=", i, "\tF=",f, "A=", acc
		print i, "\t",f, "\t", acc, "\t", thres_up, "\t", thres_down
	

def drange(start, stop, step):
	r=start
	while r < stop:
		yield r
		r += step

###############
#
# EVALUATION  #
#
###############


def evaluate2classes(test_gold, predictions, pos_label):
	f1 = f1_score(test_gold, predictions, pos_label="EQUAL")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))	
	return f1, getAccuracy(test_gold, predictions)

	
def evaluate3classes(test_gold, predictions):
	f1 = f1_score(test_gold, predictions, average="macro", 
				labels=["DIFF", "EQUAL_BAD", "EQUAL_GOOD" ])
	f1 = str(float("{0:2.2f}".format(f1 * 100)))	
	return f1, getAccuracy(test_gold, predictions)

def getAccuracy(test_gold, predictions):
	accuracy = accuracy_score(test_gold, predictions, normalize=True)
	accuracy = str(float("{0:2.2f}".format(accuracy * 100)))	
	return accuracy


#################################
#								#
# DATA READING AND PROCESSING	#
#								#
#################################

def read_data_gold(data_file):
	df = pd.read_csv(data_file)
	ids = df["qid"].values.tolist()
	cgold = df["cgold"].values.tolist()		
	return ids, cgold

def read_data_pred(data_file):	
	df = pd.read_csv(data_file, sep='\t', names=['qid', 'pred','conf'])	
	ids = df["qid"].values.tolist()
	pred = df["pred"].values.tolist()		
	conf = df["conf"].values.tolist()
	return ids, pred, conf

if __name__ == "__main__":
	main()
	
# Tried classifiers
#clf = OneVsRestClassifier(svm.SVC())
#clf = OneVsRestClassifier(lm.LogisticRegression())
#clf = OneVsRestClassifier(svm.LinearSVC(C=3), n_jobs=2)
#clf = OneVsRestClassifier(svm.SVC(kernel='linear'), n_jobs=2)
#clf = OneVsRestClassifier(svm.LinearSVC(C=1), n_jobs=2)
#clf = OneVsRestClassifier(lm.SGDClassifier())
#clf = OneVsRestClassifier(ensemble.AdaBoostClassifier(), n_jobs=2)
#clf = svm.LinearSVC(C=1, multi_class="crammer_singer", dual=False)


