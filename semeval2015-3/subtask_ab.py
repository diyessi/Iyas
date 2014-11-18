import numpy as np
import pandas as pd
import sys

from sklearn.multiclass import OneVsRestClassifier
from sklearn.multiclass import OutputCodeClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite
from sklearn.grid_search import GridSearchCV

SCALE = False

RANDOM_STATE = 123

def main():
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		subtask_a(sys.argv[1], sys.argv[2])	
		subtask_b(sys.argv[1], sys.argv[2])	

def subtask_a(train, test):
	train_ids, train_gold, train_data = read_data_subtask_a(train)
	test_ids, test_gold, test_data = read_data_subtask_a(test)
	
	if SCALE:
		ss = preprocessing.StandardScaler()
		train_data = ss.transform_fit(train_data)
		test_data = ss.transform(test_data)
	
	classifiers = []
	
	Cs = np.logspace(-5, 0, 11).tolist()
	Cs.extend([1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3])
	tolerances = [1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3]
	for c in Cs:
		for tolerance in tolerances:
			classifiers.append(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE))

	clf = GridSearchCV(estimator=OneVsRestClassifier(estimator=None), param_grid=dict(estimator=classifiers))
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)
	
	print "Best parameters with score", clf.best_score_, ":"
	print clf.best_params_
	
	with open("subtask_a.pred", "w") as out:
		for test_id, prediction in zip(test_ids, predictions):
			out.write(str(test_id) + "\t" + str(prediction) + "\n")
			
def subtask_b(train, test):
	train_ids, train_gold, train_data = read_data_subtask_b(train)
	test_ids, test_gold, test_data = read_data_subtask_b(test)
	
	if SCALE:
		ss = preprocessing.StandardScaler()
		train_data = ss.transform_fit(train_data)
		test_data = ss.transform(test_data)
	
	classifiers = []
	
	Cs = np.logspace(-5, 0, 11).tolist()
	Cs.extend([1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3])
	tolerances = [1e-3, 1e-2, 1e-1, 1e0, 1e1, 1e2, 1e3]
	for c in Cs:
		for tolerance in tolerances:
			classifiers.append(svm.LinearSVC(C=c, tol=tolerance, random_state=RANDOM_STATE))

	clf = GridSearchCV(estimator=OneVsRestClassifier(estimator=None), param_grid=dict(estimator=classifiers), cv=5)
	
	clf.fit(train_data, train_gold)
	predictions = clf.predict(test_data)
	
	print "Best parameters with score", clf.best_score_, ":"
	print clf.best_params_
	
	question_id_list = []
	question_counter = {}
	
	for question_id in test_ids:
		if question_id not in question_counter:
			question_id_list.append(question_id)
			question_counter[question_id] = {"Yes": 0, "No": 0, "Unsure": 0}
	
	for question_id, prediction in zip(test_ids, predictions):
		question_counter[question_id][prediction] += 1
	
	with open("subtask_b.pred", "w") as out:
		for question_id in question_id_list:
			
			output_label = "Unsure"
			
			counter = question_counter[question_id]
			if counter["Yes"] > counter["No"] and counter["Yes"] > counter["Unsure"]:
				output_label = "Yes"
			elif counter["No"] > counter["Yes"] and counter["No"] > counter["Unsure"]:
				output_label = "No"
			
			out.write(question_id + "\t" + output_label + "\n")


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


