package org.computer.knauss.reqtDiscussion.model.machineLearning.eval;

import java.util.Arrays;
import java.util.Comparator;

import oerich.nlputils.classifier.machinelearning.ILearningClassifier;

import org.computer.knauss.reqtDiscussion.model.Discussion;
import org.computer.knauss.reqtDiscussion.model.DiscussionEvent;
import org.computer.knauss.reqtDiscussion.model.DiscussionEventClassification;
import org.computer.knauss.reqtDiscussion.model.IClassificationFilter;
import org.computer.knauss.reqtDiscussion.model.machineLearning.eval.AbstractBucketBalancingStrategy.Bucket;
import org.computer.knauss.reqtDiscussion.model.metric.PatternMetric;

public class KFoldCrossDiscussionEvaluation {

	private static final String REFERENCE_CLASSIFIER = "gpoo,eric1";

	private AbstractBucketBalancingStrategy bucketAllocationStrategy = AbstractBucketBalancingStrategy.RANDOM_BUCKET;
	private PatternMetric patternMetric = new PatternMetric();
	private ILearningClassifier classifier;
	private IConfusionMatrixLayouter confusionMatrixLayout;

	public KFoldCrossDiscussionEvaluation() {
		// print a tex compatible table
		// this.confusionMatrixLayout = new ConfigurableLayouter(" & ",
		// "\\tabularnewline \n");
		// print an excel compatible table
		this.confusionMatrixLayout = new ConfigurableLayouter("\t", "\n");
	}

	/**
	 * Do the evaluation and create the confusion matrix.
	 * 
	 * @param k
	 *            number of buckets (default: 10).
	 * @param discussions
	 *            array with all discussions that should be used in evaluation
	 *            (default: all you have).
	 * @param aggregateBuckets
	 *            switch to aggregate dependent stories (default: true).
	 * @return confusion matrix with prediction in columns and actual values in
	 *         rows. Size is PatternMetric.PATTERNS.length + 1 (we have the
	 *         unknown value to store, too)
	 */
	public int[][] evaluate(int k, Discussion[] discussions,
			boolean aggregateBuckets) {
		Bucket[] buckets = this.bucketAllocationStrategy
				.distributedOverKBuckets(k, discussions, aggregateBuckets);

		try {
			for (int i = 0; i < buckets.length; i++) {
				System.out.println("Bucket " + i + "/10");
				this.classifier.clear();

				// use all buckets but i for training
				for (int j = 0; j < buckets.length; j++) {
					if (j != i) {
						trainClassifier(buckets[j]);
					}
				}

				classify(buckets[i]);
			}
			return evaluate(buckets).getConfusionMatrix();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void trainClassifier(Bucket bucket) {
		IClassificationFilter.NAME_FILTER.setName(REFERENCE_CLASSIFIER);

		for (Discussion[] ds : bucket) {
			for (Discussion d : ds) {
				for (DiscussionEvent de : d.getDiscussionEvents()) {
					// TODO consider to use more attributes (e.g. creator,
					// length)
					if (de.isClassified()) {
						if (de.isInClass()) {
							this.classifier.learnInClass(de.getContent());
							// System.out.print('!');
						} else {
							this.classifier.learnNotInClass(de.getContent());
							// System.out.print('.');
						}
					} else {
						// System.out.print('?');
					}
				}
				// System.out.println();
			}
		}
	}

	private void classify(Bucket bucket) {
		for (Discussion[] ds : bucket) {
			for (Discussion d : ds) {
				for (DiscussionEvent de : d.getDiscussionEvents()) {
					DiscussionEventClassification dec = new DiscussionEventClassification();

					double confidence = classifier.classify(de.getContent());
					dec.setClassifiedby(classifier.getClass().getSimpleName());
					dec.setConfidence(confidence);
					dec.setWorkitemcommentid(de.getID());

					if (classifier.getMatchValue() < confidence) {
						dec.setClassification("clarif");
						// System.out.print('!');
					} else {
						dec.setClassification("other");
						// System.out.print('.');
					}
					de.insertOrUpdateClassification(dec);
				}
				// System.out.println();
			}
		}
	}

	public void printConfusionMatrix(ConfusionMatrix confusionMatrix) {
		System.out.println(confusionMatrixLayout
				.layoutConfusionMatrix(confusionMatrix));
	}

	private ConfusionMatrix evaluate(Bucket[] buckets) {
		ConfusionMatrix confusionMatrix = new ConfusionMatrix();
		confusionMatrix.init(new String[] { "indifferent", "happy-ending",
				"discordant", "back-to-draft", "textbook-example",
				"procrastination", "unknown" });
		System.out.println("ID \t Actual \t Predicted");
		for (Bucket bucket : buckets) {
			for (Discussion[] complexTrajectory : bucket) {
				if (considerDiscussion(complexTrajectory)) {

					this.patternMetric.initDiscussions(complexTrajectory);
					IClassificationFilter.NAME_FILTER
							.setName(REFERENCE_CLASSIFIER);
					int reference = this.patternMetric.considerDiscussions(
							complexTrajectory).intValue();
					IClassificationFilter.NAME_FILTER.setName(this.classifier
							.getClass().getSimpleName());
					int classification = this.patternMetric
							.considerDiscussions(complexTrajectory).intValue();

					confusionMatrix.report(
							this.patternMetric.decode(reference),
							this.patternMetric.decode(classification));

					Arrays.sort(complexTrajectory,
							new Comparator<Discussion>() {

								@Override
								public int compare(Discussion o1, Discussion o2) {
									return ((Integer) o1.getID())
											.compareTo((Integer) o2.getID());
								}
							});

					StringBuffer line = new StringBuffer();
					for (Discussion disc : complexTrajectory) {
						line.append(disc.getID());
						line.append(',');
					}
					line.append("\t");
					line.append(this.patternMetric.decode(reference));
					line.append("\t");
					line.append(this.patternMetric.decode(classification));
					System.out.println(line.toString());
				}
			}
		}
		return confusionMatrix;
	}

	private boolean considerDiscussion(Discussion[] ds) {
		for (Discussion d : ds)
			for (DiscussionEvent de : d.getDiscussionEvents())
				if (de.isClassified())
					return true;
		return false;
	}

	public AbstractBucketBalancingStrategy getBucketAllocationStrategy() {
		return bucketAllocationStrategy;
	}

	public void setBucketAllocationStrategy(
			AbstractBucketBalancingStrategy bucketAllocationStrategy) {
		this.bucketAllocationStrategy = bucketAllocationStrategy;
	}

	public ILearningClassifier getClassifier() {
		return classifier;
	}

	public void setClassifier(ILearningClassifier classifier) {
		this.classifier = classifier;
	}

	public PatternMetric getPatternMetric() {
		return patternMetric;
	}

	public void setPatternMetric(PatternMetric patternMetric) {
		this.patternMetric = patternMetric;
	}

	public IConfusionMatrixLayouter getConfusionMatrixLayout() {
		return confusionMatrixLayout;
	}

	public void setConfusionMatrixLayout(
			IConfusionMatrixLayouter confusionMatrixLayout) {
		this.confusionMatrixLayout = confusionMatrixLayout;
	}

}