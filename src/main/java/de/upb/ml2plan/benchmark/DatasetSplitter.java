package de.upb.ml2plan.benchmark;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import weka.core.Instances;

public class DatasetSplitter {

	private static final File OUT_DIR = new File("datasets/");
	private static final File DATASET_FOLDER = new File("../datasets/classification/multi-label/");
	private static final int NUM_FOLDS = 10;

	private static final List<Long> SEEDS = Arrays.asList(42l);

	public static void main(final String[] args) throws IOException {
		OUT_DIR.mkdirs();

		for (File df : DATASET_FOLDER.listFiles()) {
			System.out.println("Create splits for dataset " + df.getName());
			Instances dataset = new Instances(new FileReader(df));
			for (long seed : SEEDS) {
				Instances copyOfDataset = new Instances(dataset);
				Collections.shuffle(copyOfDataset, new Random(seed));
				for (int i = 0; i < NUM_FOLDS; i++) {
					Instances train = copyOfDataset.trainCV(NUM_FOLDS, i);
					Instances test = copyOfDataset.testCV(NUM_FOLDS, i);

					writeDataset(df.getName(), "train", seed, i, train);
					writeDataset(df.getName(), "test", seed, i, test);
				}
			}
		}
	}

	private static final void writeDataset(final String formerFileName, final String type, final long seed, final int fold, final Instances data) throws IOException {
		String fileName = Arrays.asList(formerFileName.substring(0, formerFileName.lastIndexOf('.')), seed + "", fold + "", type).stream().collect(Collectors.joining("_")) + ".arff";
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(OUT_DIR, fileName)))) {
			bw.write(data.toString());
		}
	}

}
