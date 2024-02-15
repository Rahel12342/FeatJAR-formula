package de.featjar.formula.analysis;

import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SampleReducer {


    /**
     * Method to reduce a given set of configuration to a sample covering the same
     * t-wise interactions
     *
     * @param sample the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduce(List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        Set<Interaction> finalInteractions = getFinalInteractions(sample, t);

        int uniqueInteractions = 0;
        for (Interaction inter : finalInteractions) {
            if (inter.getCounter() == 1) {
            	boolean foundConfigurationWithUniqueInteraction = true;
                for (int i = 0; i < sample.size(); i++) {
                    BooleanSolution config = sample.get(i);
                    for(int j = 0; j < t; j++) {
                    	if ((config.get(Math.abs(inter.getContainedFeatures().get(0)) - 1)
                                == inter.getContainedFeatures().get(0))) {
                    		continue;
                    	} else {
                    		foundConfigurationWithUniqueInteraction = false;
                    		break;
                    	}
                    }
                    if(foundConfigurationWithUniqueInteraction) {
                    	reducedSample.add(config);
                    	sample.remove(config);
                    	uniqueInteractions++;
                    	break;
                    }
                }
            }
        }

        for (BooleanSolution config : reducedSample) {
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : config.get()) {
                    configAsList.add(n);
                }
            }

            for (Iterator<Interaction> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                Interaction inter = iterator.next();
                if (configAsList.containsAll(inter.getContainedFeatures())) {
                    iterator.remove();
                }
            }
        }

        BooleanSolution bestConfig = new BooleanSolution();

        Set<BooleanSolution> sampleSingle = new HashSet<>(sample);
        List<BooleanSolution> fieldConfigurations = new ArrayList<>(sampleSingle);

        Map<BooleanSolution, Double> configsWithScore = new HashMap<>();
        for (BooleanSolution config : sample) {
            configsWithScore.put(config, 0.0);
        }

        // walk through all configs and give them a score and find the best scored
        // config
        for (BooleanSolution config : fieldConfigurations) {
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : config.get()) {
                    configAsList.add(n);
                }
            }

            for (Interaction inter : finalInteractions) {
                if (configAsList.containsAll(inter.getContainedFeatures())) {
                    configsWithScore.replace(
                            config, configsWithScore.get(config) + (1.0 / (double) inter.getCounter()));
                }
            }
        }

        while (!finalInteractions.isEmpty()) {
            double bestScore = 0.0;
            for (BooleanSolution calculatedConfig : fieldConfigurations) {
                if (configsWithScore.get(calculatedConfig) > bestScore) {
                    bestScore = configsWithScore.get(calculatedConfig);
                    bestConfig = calculatedConfig;
                }
            }

            // add best config to reduced sample and remove it from unchecked configs
            reducedSample.add(bestConfig);
            fieldConfigurations.remove(bestConfig);
            configsWithScore.remove(bestConfig);

            // remove interactions that are now covered from the interations that still need
            // to be covered
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : bestConfig.get()) {
                    configAsList.add(n);
                }
            }

            List<Interaction> coveredInterations = new ArrayList<>();

            for (Iterator<Interaction> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                Interaction inter = iterator.next();
                if (configAsList.containsAll(inter.getContainedFeatures())) {
                    coveredInterations.add(inter);
                    iterator.remove();
                }
            }

            for (Interaction coveredInteraction : coveredInterations) {
                for (Iterator<BooleanSolution> iterator = fieldConfigurations.iterator(); iterator.hasNext(); ) {
                    BooleanSolution config = iterator.next();
                    List<Integer> config2AsList = new ArrayList<>();
                    {
                        for (int n : config.get()) {
                            config2AsList.add(n);
                        }
                    }

                    if (config2AsList.containsAll(coveredInteraction.getContainedFeatures())) {
                        configsWithScore.replace(
                                config,
                                configsWithScore.get(config) - (1.0 / (double) coveredInteraction.getCounter()));
                        if (configsWithScore.get(config) == 0) {
                            configsWithScore.remove(config);
                            iterator.remove();
                        }
                    }
                }
            }
        }

        return reducedSample;
    }

    public static void revertReduction(List<BooleanSolution> reducedSample, int t) {
        for (int i = reducedSample.size() - 1; i > 0; i--) {
            reducedSample.remove(i);
            computeCoverage(reducedSample, t);
        }
    }

    /**
     * This method counts how many interactions appear in a given sample
     *
     * @param sample
     * @param t
     * @return the number of interactions in the sample
     */
    private static long computeCoverage(List<BooleanSolution> sample, int t) {
        return LexicographicIterator.<Void>stream(t, sample.get(0).size() * 2)
                .map(interaction -> {
                    int[] array = new int[t];
                    for (int i = 0; i < t; i++) {
                        array[t] = interaction.elementIndices[t] > sample.get(0).size() - 1
                                ? -(interaction.elementIndices[t]
                                        - sample.get(0).size()
                                        + 1)
                                : interaction.elementIndices[t] + 1;
                    }
                    return array;
                })
                .filter(interaction -> {
                	boolean interactionsAppears = true;
                    for (BooleanSolution config : sample) {
                        for (int i = 0; i < t; i++) {
                            if (((interaction[t] < 0 && config.get(-interaction[t] - 1) == interaction[t])
                                    || (interaction[t] > 0 && config.get(interaction[t] - 1) == interaction[t]))) {
                                continue;
                            } else {
                            	interactionsAppears = false;
                                break;
                            }
                        }
                    }
                    return interactionsAppears;
                })
                .count();
    }

    /**
     * Method to reduce a given set of configuration to a t-wise sample with a
     * random apporach
     *
     * @param features a map containing features and their indices in which they are
     *                 saved in sample
     * @param sample   the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduceRandom(List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        Set<Interaction> finalInteractions = getFinalInteractions(sample, t);
        Set<Interaction> reducedInteractions = new HashSet<>();

        for (int i = 0; i < sample.size(); i++) {
            Set<Interaction> coveredInterations = new HashSet<>();
            BooleanSolution config = sample.get(i);
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : config.get()) {
                    configAsList.add(n);
                }
            }
            for (Iterator<Interaction> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                Interaction inter = iterator.next();
                if (configAsList.containsAll(inter.getContainedFeatures())) {
                    coveredInterations.add(inter);
                    iterator.remove();
                }
            }
            boolean newInteractionFound = false;
            m:
            for (Interaction inter : coveredInterations) {
                for (Interaction addedInter : reducedInteractions) {
                    if (addedInter.getContainedFeatures().containsAll(inter.getContainedFeatures())) {
                        continue m;
                    }
                }
                newInteractionFound = true;
                break;
            }
            if (newInteractionFound) {
                reducedSample.add(config);
                reducedInteractions.addAll(coveredInterations);
            }
        }

        return reducedSample;
    }

    /**
     * This method collects and counts all t-wise interactions of a given sample.
     *
     * @param sample the sample for which the interactions should be found
     * @param t
     * @return the collected interactions
     */
    private static Set<Interaction> getFinalInteractions(List<BooleanSolution> sample, int t) {
        return LexicographicIterator.<Void>stream(t, sample.get(0).size() * 2)
                .map(interaction -> {
                    int[] array = new int[t];
                    for (int i = 0; i < t; i++) {
                        array[t] = interaction.elementIndices[t] > sample.get(0).size() - 1
                                ? -(interaction.elementIndices[t]
                                        - sample.get(0).size()
                                        + 1)
                                : interaction.elementIndices[t] + 1;
                    }
                    return array;
                })
                .map((int[] interaction) -> {
                    List<Integer> interactionList = new ArrayList<>();
                    for (int i = 0; i < t; i++) {
                        interactionList.add(interaction[t]);
                    }
                    Collections.sort(interactionList);
                    return new Interaction(interactionList);
                })
                .filter((Interaction interaction) -> {
                    for (BooleanSolution config : sample) {
                        boolean foundInteraction = true;
                        for (int i = 0; i < t; i++) {
                            int feature = interaction.getContainedFeatures().get(t);
                            if (((feature < 0 && config.get(-feature - 1) == feature)
                                    || (feature > 0 && config.get(feature - 1) == feature))) {
                                continue;
                            } else {
                                foundInteraction = false;
                                break;
                            }
                        }
                        if (foundInteraction) {
                            interaction.increaseCounter();
                        }
                    }
                    return interaction.getCounter() > 0;
                })
                .collect(Collectors.toSet());
    }
}

class Interaction {

    private List<Integer> containedFeatures;
    private int counter = 0;

    public Interaction(List<Integer> containedFeatures) {
        this.containedFeatures = containedFeatures;
    }

    public void increaseCounter() {
        this.counter++;
    }

    public int getCounter() {
        return counter;
    }

    public List<Integer> getContainedFeatures() {
        return containedFeatures;
    }
}
