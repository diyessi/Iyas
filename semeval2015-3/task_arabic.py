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

RANDOM_STATE = 123

def main():
	
	random.seed(RANDOM_STATE)
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		task(sys.argv[1], sys.argv[2])	

def task(train, test):
	train_ids, train_gold, train_data = read_data(train)
	test_ids, test_gold, test_data = read_data(test)
		
	classifiers = []
	
	Cs = np.logspace(-5, 0, 11).tolist()
	for c in Cs:
		classifiers.append(OneVsRestClassifier(svm.LinearSVC(C=c)))
	for n_estimators in [120]:
		classifiers.append(OneVsRestClassifier(ensemble.AdaBoostClassifier(n_estimators=n_estimators)))

	clf = gridSearchByField(classifiers, train_ids, train_gold, train_data, n_folds=5)
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)	
	
	f1 = f1_score(test_gold, predictions, average="macro")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	
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
			print best_classifier
			print "CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%\n-\n"
	
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


