import numpy as np
import pandas as pd
import sys

from sklearn.multiclass import OneVsRestClassifier
from sklearn import svm
from sklearn import linear_model as lm
from sklearn import ensemble
from sklearn import tree
from sklearn import preprocessing
from sklearn.utils import assert_all_finite
from sklearn.grid_search import GridSearchCV

SCALE = False

def main():
	
	if len(sys.argv) != 3:
		print "Usage:", sys.argv[0], "<train file> <devel file>"
		print "You will find the script output in the current directory."
		sys.exit()
	else:
		task(sys.argv[1], sys.argv[2])	

def task(train, test):
	train_ids, train_gold, train_data = read_data(train)
	test_ids, test_gold, test_data = read_data(test)
	
	if SCALE:
		ss = preprocessing.StandardScaler()
		train_data = ss.transform_fit(train_data)
		test_data = ss.transform(test_data)
		
	classifiers = [ensemble.AdaBoostClassifier(n_estimators=120)]
	
	Cs = np.logspace(-5, 0, 11)
	for c in Cs:
		classifiers.append(svm.LinearSVC(C=c))

	clf = GridSearchCV(estimator=OneVsRestClassifier(estimator=None), param_grid=dict(estimator=classifiers))
	
	clf.fit(train_data, train_gold)
	
	predictions = clf.predict(test_data)
	
	print "Best parameters with score", clf.best_score_, ":"
	print clf.best_params_
	
	with open("task_arabic.pred", "w") as out:
		for test_id, prediction in zip(test_ids, predictions):
			out.write(str(test_id) + "\t" + str(prediction) + "\n")

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


