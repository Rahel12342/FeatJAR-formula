package de.featjar.formula.analysis;

import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SampleReducer {

    static int partCount = 0;

    static class Environment {
        final int[] literals;
        final int[] containingConfigs;
        double[] scores;
        int[] uniqueInteractions;

        public Environment(int t, int configLength) {
            literals = new int[t];
            containingConfigs = new int[configLength];
        }

        public void reset(int configLength) {
            scores = new double[configLength];
            uniqueInteractions = new int[configLength];
        }
    }

    public static List<BooleanSolution> reduce(List<BooleanSolution> sample, int t) {
        if (sample.isEmpty()) {
            return new ArrayList<>();
        }
        sample = new ArrayList<>(sample);
        final int n = sample.get(0).size();
        //        int largestSampleSize = 0;
        //        for(BooleanSolution config : sample) {
        //        	if (config.size() > largestSampleSize) {
        //        		largestSampleSize = config.size();
        //        	}
        //        }
        //        final int n = largestSampleSize;
        int t2 = (n < t) ? n : t;
        final int pow = (int) Math.pow(2, t2);
        final int configLength = sample.size();
        List<Environment> envs = new ArrayList<>();
        partCount = 0;
        int reducedConfigCount = 0;

        while (true) {
            int remainingConfigCount = configLength - reducedConfigCount;
            List<BooleanSolution> remeiningSample = sample.subList(0, remainingConfigCount);
            List<BooleanSolution> reducedSample = sample.subList(remainingConfigCount, sample.size());
            for (Environment environment : envs) {
                environment.reset(configLength);
            }

            LexicographicIterator.parallelStream(t2, n).forEach(combo -> {
                if (partCount <= combo.spliteratorId) {
                    synchronized (envs) {
                        for (int i = partCount; i <= combo.spliteratorId; i++) {
                            Environment e = new Environment(t2, configLength);
                            e.reset(configLength);
                            envs.add(e);
                            partCount++;
                        }
                    }
                }
                Environment environment = envs.get(combo.spliteratorId);

                for (int i = 0; i < pow; i++) {
                    for (int k = 0; k < environment.literals.length; k++) {
                        environment.literals[k] =
                                (i >> k & 1) == 0 ? (combo.elementIndices[k] + 1) : -(combo.elementIndices[k] + 1);
                    }

                    if (reducedSample.stream().noneMatch(config -> config.containsAll(environment.literals))) {
                        int count = 0;
                        for (int j = 0; j < remainingConfigCount; j++) {
                            if (remeiningSample.get(j).containsAll(environment.literals)) {
                                environment.containingConfigs[count++] = j;
                            }
                        }

                        if (count > 0) {
                            double score = 1.0 / count;
                            for (int j = 0; j < count; j++) {
                                environment.scores[environment.containingConfigs[j]] += score;
                            }
                            if (count == 1) {
                                for (int j = 0; j < count; j++) {
                                    environment.uniqueInteractions[environment.containingConfigs[j]]++;
                                }
                            }
                        }
                    }
                }
            });

            final int[] uniqueInterationsTotal = new int[configLength];
            for (Environment env : envs) {
                for (int i = 0; i < env.uniqueInteractions.length; i++) {
                    uniqueInterationsTotal[i] += env.uniqueInteractions[i];
                }
            }
            int bestConfigIndex = -1;
            int maxUniqueInterations = 0;
            for (int i = 0; i < remainingConfigCount; i++) {
                if (maxUniqueInterations < uniqueInterationsTotal[i]) {
                    maxUniqueInterations = uniqueInterationsTotal[i];
                    bestConfigIndex = i;
                }
            }
            if (bestConfigIndex > -1) {
                reducedConfigCount++;
                Collections.swap(sample, bestConfigIndex, configLength - reducedConfigCount);
                continue;
            }

            final double[] scoresTotal = new double[configLength];
            for (Environment env : envs) {
                for (int i = 0; i < env.scores.length; i++) {
                    scoresTotal[i] += env.scores[i];
                }
            }
            double maxScore = 0;
            for (int i = 0; i < remainingConfigCount; i++) {
                if (maxScore < scoresTotal[i]) {
                    maxScore = scoresTotal[i];
                    bestConfigIndex = i;
                }
            }
            if (bestConfigIndex > -1) {
                reducedConfigCount++;
                Collections.swap(sample, bestConfigIndex, configLength - reducedConfigCount);
                continue;
            }
            break;
        }
        return new ArrayList<>(sample.subList(configLength - reducedConfigCount, sample.size()));
    }
}
