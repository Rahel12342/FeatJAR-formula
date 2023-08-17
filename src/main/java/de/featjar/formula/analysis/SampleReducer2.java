package de.featjar.formula.analysis;

import de.featjar.formula.analysis.bool.BooleanSolution;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SampleReducer2 {

    /**
     * Method to reduce a given set of configuration to a t-wise sample with a approach using a score
     *
     * @param features a map containing features and their indices in which they are saved in sample
     * @param sample the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduce(Map<String, Integer> features, List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        Set<List<Integer>> interactions = new HashSet<>();
        Set<Interaction> finalInteractions = new HashSet<>();

        long start = System.currentTimeMillis();

        // TODO nochmal überprüfen obs das mit (t-1) & Co so richtig ist
        for (BooleanSolution config : sample) {
            for (int i = 0; i < config.size() - (t - 1); i++) {
                for (int j = i + 1; j < config.size() - (t - 2); j++) {
                    List<Integer> newInteraction = new ArrayList<>();
                    newInteraction.add(config.get(i));
                    for (int m = j; m < j + (t - 1); m++) {
                        newInteraction.add(config.get(m));
                    }
                    Collections.sort(newInteraction);
                    if (!interactions.add(newInteraction)) {
                        for (Interaction inter : finalInteractions) {
                            if (inter.containedFeatures.containsAll(newInteraction)) {
                                inter.increaseCounter();
                                break;
                            }
                        }
                    } else {
                        finalInteractions.add(new Interaction(newInteraction));
                    }
                }
            }
        }

        System.out.println("Size contained Interactions calculated from Rahel:" + finalInteractions.size());
        BooleanSolution bestConfig = new BooleanSolution();

        int counter = 1;

        while (true) {

            Map<BooleanSolution, Double> configsWithScore = new HashMap<>();
            for (BooleanSolution config : sample) {
                configsWithScore.put(config, 0.0);
            }

            double bestScore = 0.0;

            // walk through all configs and give them a score and find the best scored config
            m:
            for (BooleanSolution config : sample) {
                List<Integer> configAsList = new ArrayList<>();
                {
                    for (int n : config.get()) {
                        configAsList.add(n);
                    }
                }

                for (Interaction inter : finalInteractions) {
                    if (configAsList.containsAll(inter.containedFeatures)) {
                        if (inter.counter == 1) {
                            bestConfig = config;
                            bestScore = 100;
                            break m;
                        } else {
                            configsWithScore.replace(
                                    config, configsWithScore.get(config) + (1.0 / (double) inter.counter));
                        }
                    }
                }
            }

            if (bestScore != 100) {
                for (BooleanSolution calculatedConfig : sample) {
                    if (configsWithScore.get(calculatedConfig) > bestScore) {
                        bestScore = configsWithScore.get(calculatedConfig);
                        bestConfig = calculatedConfig;
                    }
                }
            }

            // add best config to reduced sample and remove it from unchecked configs
            reducedSample.add(bestConfig);
            sample.remove(bestConfig);

            // remove interactions that are now covered from the interations that still need to be covered
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : bestConfig.get()) {
                    configAsList.add(n);
                }
            }

            for (Iterator<Interaction> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                Interaction inter = iterator.next();
                if (configAsList.containsAll(inter.containedFeatures)) {
                    iterator.remove();
                }
            }

            System.out.println(counter + ". Durchlauf; größe der übrigen Interaktionen: " + finalInteractions.size()
                    + "; Größe der übrigen Konfigs: " + sample.size() + "Größe des finalen samples: "
                    + reducedSample.size());
            counter++;
            if (finalInteractions.isEmpty()) {
                break;
            }
        }

        return reducedSample;
    }

    /**
     * Method to reduce a given set of configuration to a t-wise sample with a random apporach
     *
     * @param features a map containing features and their indices in which they are saved in sample
     * @param sample the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduceRandom(
            Map<String, Integer> features, List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();

        return reducedSample;
    }

    /**
     * Method to reduce a given set of configuration to a t-wise sample with a approach using a score
     *
     * @param features a map containing features and their indices in which they are saved in sample
     * @param sample the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduceAndEvaluateTime(
            Map<String, Integer> features, List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        Set<List<Integer>> interactions = new HashSet<>();
        Set<Interaction> finalInteractions = new HashSet<>();

        long start = System.currentTimeMillis();

        // TODO nochmal überprüfen obs das mit (t-1) & Co so richtig ist
        for (BooleanSolution config : sample) {
            for (int i = 0; i < config.size() - (t - 1); i++) {
                for (int j = i + 1; j < config.size() - (t - 2); j++) {
                    List<Integer> newInteraction = new ArrayList<>();
                    newInteraction.add(config.get(i) == 0 ? -(i + 1) : config.get(i));
                    for (int m = j; m < j + (t - 1); m++) {
                        newInteraction.add(config.get(m) == 0 ? -(m + 1) : config.get(m));
                    }
                    Collections.sort(newInteraction);
                    if (!interactions.add(newInteraction)) {
                        for (Interaction inter : finalInteractions) {
                            if (inter.containedFeatures.containsAll(newInteraction)) {
                                inter.increaseCounter();
                                break;
                            }
                        }
                    } else {
                        finalInteractions.add(new Interaction(newInteraction));
                    }
                }
            }
        }

        long endFindInteractions = System.currentTimeMillis() - start;
        System.out.println("Time Finding all interactions: " + endFindInteractions / 1000.0 + " s");
        System.out.println("Size contained Interactions calculated from Rahel:" + finalInteractions.size());
        BooleanSolution bestConfig = new BooleanSolution();

        long endRemoveInteractions = System.currentTimeMillis() - endFindInteractions;
        int counter = 1;

        while (true) {

            Map<BooleanSolution, Double> configsWithScore = new HashMap<>();
            for (BooleanSolution config : sample) {
                configsWithScore.put(config, 0.0);
            }

            long endGiveConfigsZeroScore = System.currentTimeMillis() - endRemoveInteractions;
            //            System.out.println("Time Giving the scores: " + endGiveConfigsZeroScore / 1000.0 + " s");

            double bestScore = 0.0;

            // walk through all configs and give them a score and find the best scored config
            m:
            for (BooleanSolution config : sample) {
                List<Integer> configAsList = new ArrayList<>();
                {
                    for (int n : config.get()) {
                        configAsList.add(n);
                    }
                }

                for (Interaction inter : finalInteractions) {
                    if (configAsList.containsAll(inter.containedFeatures)) {
                        if (inter.counter == 1) {
                            bestConfig = config;
                            bestScore = 100;
                            break m;
                        } else {
                            configsWithScore.replace(
                                    config, configsWithScore.get(config) + (1.0 / (double) inter.counter));
                        }
                    }
                }
            }

            long endGoThroughInteractions = System.currentTimeMillis() - endGiveConfigsZeroScore;
            //    		System.out.println("Time Finding all interactions: " + endGoThroughInteractions / 1000.0 + " s");
            if (bestScore != 100) {
                for (BooleanSolution calculatedConfig : sample) {
                    if (configsWithScore.get(calculatedConfig) > bestScore) {
                        bestScore = configsWithScore.get(calculatedConfig);
                        bestConfig = calculatedConfig;
                    }
                }
            }
            long endFindBestScoredConfig = System.currentTimeMillis() - start;
            //    		System.out.println("Time Finding best scored Config: " + endFindBestScoredConfig / 1000.0 + " s");

            // add best config to reduced sample and remove it from unchecked configs
            reducedSample.add(bestConfig);
            sample.remove(bestConfig);

            // remove interactions that are now covered from the interations that still need to be covered
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : bestConfig.get()) {
                    configAsList.add(n);
                }
            }

            for (Iterator<Interaction> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                Interaction inter = iterator.next();
                if (configAsList.containsAll(inter.containedFeatures)) {
                    iterator.remove();
                }
            }

            endRemoveInteractions = System.currentTimeMillis() - endFindBestScoredConfig;
            //            System.out.println("Time removing covered interactions: " + endRemoveInteractions / 1000.0 + "
            // s");
            System.out.println(counter + ". Durchlauf; größe der übrigen Interaktionen: " + finalInteractions.size()
                    + "; Größe der übrigen Konfigs: " + sample.size() + "Größe des finalen samples: "
                    + reducedSample.size());
            counter++;
            if (finalInteractions.isEmpty()) {
                break;
            }
        }

        return reducedSample;
    }
}

class Interaction {

    List<Integer> containedFeatures;
    int counter = 1;

    public Interaction(List<Integer> containedFeatures) {
        this.containedFeatures = containedFeatures;
    }

    public void increaseCounter() {
        this.counter++;
    }
}
