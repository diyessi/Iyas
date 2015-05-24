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
from numpy import average
from Crypto.Util.RFC1751 import binary

RANDOM_STATE = 123

THREE_CLASSES= False
BOOL_RBF = False

LINEAR_SUFFIX = ".lin.2.normalised.pred"
RBF_SUFFIX = ".rbf.3.normalised.pred"

"""
We implement the classifier for the dataset in which comments from the same
thread are paired together and the task consists of predicting whether t hey 
belong to the same class or not.
"""

def main():
	
	random.seed(RANDOM_STATE)
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		#TODO no current directory, but the devel file with an extension
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		print "INPUT FILES: ", sys.argv[1], sys.argv[2]
		train_and_test_model(sys.argv[1], sys.argv[2])	
		#subtask_b(sys.argv[1], sys.argv[2])	

def train_and_test_model(train, test):
	train_ids, train_gold, train_data = read_data_subtask_a(train)
	test_ids, test_gold, test_data = read_data_subtask_a(test)

	if BOOL_RBF:
		classifiers = getRbfClassifiers()
		clf = gridSearchByField_rbf(classifiers, train_ids, train_gold, train_data, n_folds=5)
	else:
		classifiers = getLinearClassifiers()
		clf = gridSearchByField_linear(classifiers, train_ids, train_gold, train_data, n_folds=5)
	
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)
	scores = clf.decision_function(test_data)
	
	if THREE_CLASSES:
		f, acc = evaluate3classes(test_gold, predictions)
	else:
		f, acc = evaluate2classes(test_gold, predictions)
	
	with open(test+getSuffix() , "w") as out:
		for test_id, prediction, score in zip(test_ids, predictions, scores):
			out.write(str(test_id) + "\t" + str(prediction) + "\t" + str(score) + "\n" )


	


def getSuffix():
	if BOOL_RBF:
		return RBF_SUFFIX
	return LINEAR_SUFFIX


def getLinearClassifiers():
	classifiers = []
	
	#  best hyperparams:
	#  C = 10.0
	#  tol = 0.001
	#  CV F1: 21.46%
	print "RUNNING WITH linear KERNEL"
	Cs = np.logspace(-5, 0, 11).tolist()
	Cs.extend([1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3])
	tolerances = [1e-3]
	for c in Cs:
		for tolerance in tolerances:
			if THREE_CLASSES:
				classifiers.append(OneVsRestClassifier(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE, 
													class_weight ='auto', verbose=True), n_jobs=-1))

			else:
				classifiers.append(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE, 
											class_weight ='auto', verbose=True))
	return classifiers

def getRbfClassifiers():
	classifiers = []
	
	#  best hyperparams:
	#  C = 10.0
	#  tol = 0.001
	#  CV F1: 21.46%
	print "RUNNING WITH RBF KERNEL"
	Cs = np.logspace(-5, 0, 11).tolist()
	
	gammas = [1]
	
	for g in gammas:
		if THREE_CLASSES:
				classifiers.append(OneVsRestClassifier(svm.SVC(C=1.0, gamma=g, class_weight='auto', 
								verbose=True, max_iter=1000), n_jobs=-1))
		else:
			classifiers.append(svm.SVC(C=1.0, gamma=g, class_weight='auto', 
								verbose=True, max_iter=1000))
	return classifiers

def gridSearchByField_linear(classifiers, train_ids, train_gold, train_data, n_folds=5):
	ids = [id.split("_")[0] for id in train_ids]
	unique_ids = list(set(ids))
	random.shuffle(unique_ids)
	
	best_classifier = None
	best_f1 = 0.0
	
	for clf in classifiers:


		if THREE_CLASSES:
			print "[TRAINING]:", clf.estimator.__class__.__name__, "with: C =", \
				clf.estimator.C, "tol =", clf.estimator.tol
		else:
 			print "[TRAINING]: with: C =", \
 				clf.C, "tol =", clf.tol,
		
		f1s = []
		
		for fold_ids in slice_it(unique_ids, n_folds):
			train_indexes = [id not in fold_ids for id in ids]
			test_indexes = [id in fold_ids for id in ids]
			
			train = train_data[train_indexes]
			train_labels = [label for label, include_it in zip(train_gold, train_indexes) if include_it]
			
			test = train_data[test_indexes]
			test_labels = [label for label, include_it in zip(train_gold, test_indexes) if include_it]

			clf.fit(train, train_labels)
			predicted_labels = clf.predict(test)
			f1 = f1_score(test_labels, predicted_labels, pos_label="EQUAL")
			
			f1s.append(f1)

		mean_f1 = np.mean(f1s)
		
		if mean_f1 > best_f1:
			best_classifier = clf
			best_f1 = mean_f1
			if THREE_CLASSES:
				print "[SELECTED]:", clf.estimator.__class__.__name__, \
				"with: C =", clf.estimator.C, "tol =", clf.estimator.tol, \
				"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
			else:
				print "[SELECTED]:", clf.__class__.__name__, \
					"with: C =", clf.C, "tol =", clf.tol, \
					"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
	
	return best_classifier



def gridSearchByField_rbf(classifiers, train_ids, train_gold, train_data, n_folds=5):
	ids = [id.split("_")[0] for id in train_ids]
	unique_ids = list(set(ids))
	random.shuffle(unique_ids)
	
	best_classifier = None
	best_f1 = 0.0
	
	for clf in classifiers:


		print "[TRAINING]:", getClass(clf), "with: C =", \
				getC(clf), "Gamma =",getG(clf), "tol =", getTol(clf)
		
		
		f1s = []
		
		for fold_ids in slice_it(unique_ids, n_folds):
			train_indexes = [id not in fold_ids for id in ids]
			test_indexes = [id in fold_ids for id in ids]
			
			train = train_data[train_indexes]
			train_labels = [label for label, include_it in zip(train_gold, train_indexes) if include_it]
			
			test = train_data[test_indexes]
			test_labels = [label for label, include_it in zip(train_gold, test_indexes) if include_it]

			clf.fit(train, train_labels)
			predicted_labels = clf.predict(test)
			f1 = f1_score(test_labels, predicted_labels, pos_label="EQUAL")
			
			f1s.append(f1)

		mean_f1 = np.mean(f1s)
		
		if mean_f1 > best_f1:
			best_classifier = clf
			best_f1 = mean_f1
			if THREE_CLASSES:
				print "[SELECTED]:", clf.estimator.__class__.__name__, \
				"with: C =", getC(clf), "Gamma=", getG(clf), "tol =", clf.estimator.tol, \
				"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
			else:
				print "[SELECTED]:", 
	
	return best_classifier
	
def getClass(clf):	
	return clf.estimator.__class__.__name__ if THREE_CLASSES else clf.__class__.__name__

def getC(clf):
	return clf.estimator.C if THREE_CLASSES else clf.C

def getG(clf):
	return clf.estimator.gamma if THREE_CLASSES else clf.gamma 

def getTol(clf):
	return clf.estimator.tol if THREE_CLASSES else clf.tol
	


###############
#
# EVALUATION  #
#
###############


def evaluate2classes(test_gold, predictions):
	f1 = f1_score(test_gold, predictions, pos_label="EQUAL", average="macro")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1 macro:", f1 + "%"
	
	
	f1 = f1_score(test_gold, predictions, pos_label="EQUAL", average="binary")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	return f1, getAccuracy(test_gold, predictions)

	
def evaluate3classes(test_gold, predictions):
	f1 = f1_score(test_gold, predictions, average="macro", 
				labels=["DIFF", "EQUAL_BAD", "EQUAL_GOOD" ])
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	return f1, getAccuracy(test_gold, predictions)

def getAccuracy(test_gold, predictions):	
	accuracy = accuracy_score(test_gold, predictions, normalize=True)
	accuracy = str(float("{0:2.2f}".format(accuracy * 100)))
	
	print "Script ACCURACY:", accuracy + "%"	
	return accuracy


#################################
#								#
# DATA READING AND PROCESSING	#
#								#
#################################

def slice_it(li, cols=2):
	start = 0
	for i in xrange(cols):
		stop = start + len(li[i::cols])
		yield li[start:stop]
		start = stop

def read_data_subtask_a(data_file):
	df = pd.read_csv(data_file)
	ids = df["qid"].values
	cgold = df["cgold"].values.tolist()	
	data = df[range(2, df.shape[1])]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	if BOOL_RBF:
		#data normalization
		#Simone suggestion
		#data = (data - data.min()) / (data.max() - data.min())
		#Giovanni suggestion		
		data = (data - data.min()) / (data.max())
		data = data.replace([np.inf, -np.inf, np.nan], 0.0)
		#scikit option
		#data =preprocessing.scale(data)
		
	return ids, cgold, data


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


