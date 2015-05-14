import sys
import random
import math

import numpy as np
import pandas as pd
import scipy as sp

from sklearn.multiclass import OneVsRestClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite
from sklearn.metrics import f1_score, accuracy_score
from sklearn.grid_search import GridSearchCV
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn import cluster
from sklearn import lda

RANDOM_STATE = 123

def main():
	
	random.seed(RANDOM_STATE)
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		task_hamdy(sys.argv[1], sys.argv[2])	
			
def task_hamdy(train, test):
	train_ids, train_gold, train_data = read_data(train)
	test_ids, test_gold, test_data = read_data(test)
	
	target_label = "direct"
	
	train_gold = [1 if label == target_label else 0 for label in train_gold]
	test_gold = [1 if label == target_label else 0 for label in test_gold]
		
	classifiers = []
	
	Cs = range(1, 101)
	for c in Cs:
		for tol in [0.0001]:
			classifiers.append(lm.LogisticRegression(C=c, tol=tol, random_state=RANDOM_STATE))
	
	clf = gridSearchByField(classifiers, train_ids, train_gold, train_data, n_folds=10)
	
	train_data, test_data = add_hamdy_predictions("data/SemEval2015-Task3-Arabic-data/datasets/hamdy-features-last/QA-Arabic-result-NewTrain-Scores.txt", \
		"data/SemEval2015-Task3-Arabic-data/datasets/hamdy-features-last/QA-Arabic-result-Test-Scores.txt", train_data, test_data)
	
	clf.fit(train_data, train_gold)
	scores = clf.decision_function(test_data)
	
	id_to_pred = {test_id.split("-")[0] : [] for test_id in test_ids}
	
	test_id_to_label = {}
	predictions = []
	
	for test_id, score in zip(test_ids, scores):
		full_id = test_id
		test_id = test_id.split("-")[0]
		id_to_pred[test_id].append((score, len(id_to_pred[test_id]), full_id))
		
	for test_id in id_to_pred:
		id_to_pred[test_id].sort(key=lambda tup: tup[0], reverse=True)
		direct_full_id = id_to_pred[test_id][0][2]
		test_id_to_label[direct_full_id] = "direct"
		if len(id_to_pred[test_id]) > 1:
			related_full_id = id_to_pred[test_id][1][2]
			test_id_to_label[related_full_id] = "related"
			
	predictions = []
	for test_id in test_ids:
		if test_id in test_id_to_label:
			predictions.append(test_id_to_label[test_id])
		else:
			predictions.append("irrelevant")
	
	with open("task_arabic.pred", "w") as out:
		for test_id, prediction in zip(test_ids, predictions):
			out.write(str(test_id) + "\t" + str(prediction) + "\n")
			
def gridSearchByField(classifiers, train_ids, train_gold, train_data, n_folds=5):
	ids = [id.split("-")[0] for id in train_ids]
	unique_ids = list(set(ids))
	random.shuffle(unique_ids)
	
	best_classifier = None
	best_f1 = 0.0
	
	for clf in classifiers:
		
		if hasattr(clf, "C"):
			print "[TRAINING]:", clf.__class__.__name__, "with: C =", clf.C, "tol =", clf.tol
		
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
			f1 = f1_score(test_labels, predicted_labels, average="macro")
			
			f1s.append(f1)

		mean_f1 = np.mean(f1s)
		
		if mean_f1 > best_f1:
			best_classifier = clf
			best_f1 = mean_f1
			if hasattr(clf, "C"):
				print "[SELECTED]:", clf.__class__.__name__, "with: C =", clf.C, "tol =", clf.tol, \
					"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
	
	return best_classifier
	
def slice_it(li, cols=2):
	start = 0
	for i in xrange(cols):
		stop = start + len(li[i::cols])
		yield li[start:stop]
		start = stop	

def read_data(data_file):
	df = pd.read_csv(data_file)
	ids = df["cid"].values
	cgold = df["cgold"].values.tolist()
	data = df[range(3, df.shape[1])]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	
	return ids, cgold, data
	
def add_hamdy_predictions(train_file, dev_file, train_data, dev_data):
	train_pred = pd.read_csv(train_file, delimiter="\t", header=None)
	dev_pred = pd.read_csv(dev_file, delimiter="\t", header=None)
	
	train_pred.columns = ["QID", "LABEL", "SCORE"]
	dev_pred.columns = ["QID", "LABEL", "SCORE"]
	
	train_pred = pd.get_dummies(train_pred["LABEL"])
	dev_pred = pd.get_dummies(dev_pred["LABEL"])
	
	for column in train_pred.columns:
		train_feature = [[pred] for pred in train_pred[column].values.tolist()]
		dev_feature = [[pred] for pred in dev_pred[column].values.tolist()]
		train_data = sp.sparse.hstack((train_data, train_feature))
		dev_data = sp.sparse.hstack((dev_data, dev_feature))
	
	return train_data, dev_data

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


