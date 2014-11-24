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
		subtask_a(sys.argv[1], sys.argv[2])	
		#subtask_b(sys.argv[1], sys.argv[2])	

def subtask_a(train, test):
	train_ids, train_gold, train_data = read_data_subtask_a(train)
	test_ids, test_gold, test_data = read_data_subtask_a(test)
	
	classifiers = []
	
	#  best hyperparams:
	#  C = 10.0
	#  tol = 0.001
	#  CV F1: 21.46%
	
	Cs = np.logspace(-5, 0, 11).tolist()
	Cs.extend([1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3])
	tolerances = [1e-3]
	for c in Cs:
		for tolerance in tolerances:
			classifiers.append(OneVsRestClassifier(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE), n_jobs=2))

	clf = gridSearchByField_a(classifiers, train_ids, train_gold, train_data, n_folds=5)
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)
	
	f1 = f1_score(test_gold, predictions, average="macro")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	
	dialogue_ids = set([line.strip() for line in open("data/"
			+ "SemEval2015-Task3-English-data/datasets/CQA-QL-devel.xml.dialogue.txt", "r")])
	
	with open("subtask_a.pred", "w") as out:
		for test_id, prediction in zip(test_ids, predictions):
			if test_id in dialogue_ids:
				prediction = "Dialogue"
			out.write(str(test_id) + "\t" + str(prediction) + "\n")
			
def gridSearchByField_a(classifiers, train_ids, train_gold, train_data, n_folds=5):
	ids = [id.split("_")[0] for id in train_ids]
	unique_ids = list(set(ids))
	random.shuffle(unique_ids)
	
	best_classifier = None
	best_f1 = 0.0
	
	for clf in classifiers:
		
		print "[TRAINING]:", clf.estimator.__class__.__name__, "with: C =", \
			clf.estimator.C, "tol =", clf.estimator.tol
		
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
			print "[SELECTED]:", clf.estimator.__class__.__name__, \
				"with: C =", clf.estimator.C, "tol =", clf.estimator.tol, \
				"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
	
	return best_classifier
			
def subtask_b(train, test):
	train_ids, train_gold, train_data = read_data_subtask_b(train)
	test_ids, test_gold, test_data = read_data_subtask_b(test)
	
	classifiers = []
	
	#  best hyperparams:
	#  C = 1.0
	#  tol = 10.0
	#  CV F1: 34.26%
	
	Cs = np.logspace(-5, 0, 11).tolist()
	Cs.extend([1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3])
	tolerances = [1e1]
	for c in Cs:
		for tolerance in tolerances:
			classifiers.append(OneVsRestClassifier(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE), n_jobs=1))

	clf = gridSearchByField_b(classifiers, train_ids, train_gold, train_data, n_folds=5)
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)
	
	question_id_list = []
	question_counter = {}
	question_gold = []
	
	for index, question_id in enumerate(test_ids):
		if question_id not in question_counter:
			question_id_list.append(question_id)
			question_counter[question_id] = {"Yes": 0, "No": 0, "Unsure": 0}
	
	for question_id, prediction in zip(test_ids, predictions):
		question_counter[question_id][prediction] += 1
		
	question_gold = [line.strip().split("\t")[1] for line in \
		open("data/SemEval2015-Task3-English-data/datasets/CQA-QL-devel-gold-yn.txt", "r")]
		
	output_labels = []
	
	for question_id in question_id_list:
		output_label = "Unsure"
			
		counter = question_counter[question_id]
		if counter["Yes"] > counter["No"] and counter["Yes"] > counter["Unsure"]:
			output_label = "Yes"
		elif counter["No"] > counter["Yes"] and counter["No"] > counter["Unsure"]:
			output_label = "No"
			
		output_labels.append(output_label)
		
	f1 = f1_score(question_gold, output_labels, average="macro")
	f1 = str(float("{0:2.2f}".format(f1 * 100)))
	
	print "Script F1:", f1 + "%"
	
	with open("subtask_b.pred", "w") as out:
		for question_id, output_label in zip(question_id_list, output_labels):
			out.write(question_id + "\t" + output_label + "\n")
	
def gridSearchByField_b(classifiers, train_ids, train_gold, train_data, n_folds=5):
	ids = train_ids
	unique_ids = list(set(ids))
	random.shuffle(unique_ids)
	
	best_classifier = None
	best_f1 = 0.0
	
	for clf in classifiers:
		
		print "[TRAINING]:", clf.estimator.__class__.__name__, "with: C =", \
			clf.estimator.C, "tol =", clf.estimator.tol
		
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
			print "[SELECTED]:", clf.estimator.__class__.__name__, \
				"with: C =", clf.estimator.C, "tol =", clf.estimator.tol, \
				"| CV F1: " + str(float("{0:2.2f}".format(best_f1 * 100))) + "%"
	
	return best_classifier
	
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
	cgold_yn = df["cgold_yn"].values.tolist()
	data = df[range(3, df.shape[1])]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	
	return ids, cgold, data
	
def read_data_subtask_b(data_file):
	df = pd.read_csv(data_file)
	
	df = df[df["cgold_yn"] != "Not Applicable"]
	
	ids = [id.split("_")[0] for id in df["qid"].values]
	cgold = df["cgold"].values.tolist()
	cgold_yn = df["cgold_yn"].values.tolist()
	data = df[range(3, df.shape[1])]
	data = data.astype("float64").replace([np.inf, -np.inf, np.nan], 0.0)
	
	return ids, cgold_yn, data

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


