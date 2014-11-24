import numpy as np
import pandas as pd
import sys
import random

from sklearn.multiclass import OneVsRestClassifier, OneVsOneClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite
from sklearn.metrics import f1_score, accuracy_score
from sklearn.grid_search import GridSearchCV

RANDOM_STATE = 123

def main():
	
	random.seed(RANDOM_STATE)
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		task_hc(sys.argv[1], sys.argv[2])	

def task(train, test):
	train_ids, train_gold, train_data = read_data(train)
	test_ids, test_gold, test_data = read_data(test)
		
	classifiers = []
	
	Cs = np.logspace(0, 1, 20).tolist()
	for c in Cs:
		classifiers.append(OneVsRestClassifier(svm.LinearSVC(C=c, random_state=RANDOM_STATE), n_jobs=2))

	clf = gridSearchByField(classifiers, train_ids, train_gold, train_data, n_folds=5)
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)	
	
	f1 = f1_score(test_gold, predictions, average="macro")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	
	with open("task_arabic.pred", "w") as out:
		for test_id, prediction in zip(test_ids, predictions):
			out.write(str(test_id) + "\t" + str(prediction) + "\n")
			
def task_hc(train, test):
	train_ids, train_gold, train_data = read_data(train)
	test_ids, test_gold, test_data = read_data(test)
		
	classifiers = []
	
	Cs = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
	for c in Cs:
		classifiers.append(svm.LinearSVC(C=c, random_state=RANDOM_STATE))
		
	train_gold_lvl_1 = [1 if label == "direct" or label == "related" else 0 for label in train_gold]
	
	clf_1 = gridSearchByField(classifiers, train_ids, train_gold_lvl_1, train_data, n_folds=5)
	clf_1.fit(train_data, train_gold_lvl_1)
	
	###################################################################
	
	classifiers = []
	
	Cs = [0.001, 0.01, 0.1, 1, 10, 100, 1000]
	for c in Cs:
		classifiers.append(svm.LinearSVC(C=c, random_state=RANDOM_STATE))
	
	train_data["ids"] = train_ids
	train_data["gold"] = train_gold
	train_data = train_data[train_data["gold"] != "irrelevant"]
	
	train_ids = train_data["ids"].values.tolist()
	train_gold = train_data["gold"].values.tolist()
	train_data = train_data.drop("ids", 1)
	train_data = train_data.drop("gold", 1)
	
	train_gold = [1 if label == "direct" else 0 for label in train_gold]
	
	clf_2 = gridSearchByField(classifiers, train_ids, train_gold, train_data, n_folds=5)
	clf_2.fit(train_data, train_gold)
	
	predictions = []
	
	for index in xrange(test_data.shape[0]):
		row = test_data[index:index+1]
		prediction = clf_1.predict(row)
		if prediction == 1:
			prediction = clf_2.predict(row)
			if prediction == 1:
				prediction = "direct"
			else:
				prediction = "related"
		else:
			prediction = "irrelevant"
		predictions.append(prediction)
	
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


