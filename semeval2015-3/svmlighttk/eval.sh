python produce_submissions.py

echo "Producing report for subtask A..."
perl ../SemEval2015-task3-scorer-subtaskA.pl \
	../data/SemEval2015-Task3-English-data/datasets/CQA-QL-devel-gold.txt subtask_a.pred > subtask_a.report

echo "Producing report for subtask B..."
perl ../SemEval2015-task3-scorer-subtaskB.pl \
	../data/SemEval2015-Task3-English-data/datasets/CQA-QL-devel-gold-yn.txt subtask_b.pred > subtask_b.report

echo "Subtask A)" `tail subtask_a.report -n 1`
echo "Subtask B)" `tail subtask_b.report -n 1`
