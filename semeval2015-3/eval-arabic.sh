echo "Training model and outputting predictions..."
python task_arabic.py data/SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-train.xml.csv \
	data/SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.xml.csv

echo "Producing report for the Arabic task..."
perl SemEval2015-task3-scorer-subtaskA.pl \
	data/SemEval2015-Task3-Arabic-data/datasets/QA-Arabic-dev.gold task_arabic.pred > task_arabic.report

