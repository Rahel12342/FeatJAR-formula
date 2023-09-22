package de.featjar.formula.analysis;

import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SampleReducer2 {

    /**
     * Method to reduce a given set of configuration to a t-wise sample with a
     * approach using a score
     *
     * @param features a map containing features and their indices in which they are
     *                 saved in sample
     * @param sample   the set of configuration that will be reduced
     * @param t
     * @return the reduced sample
     */
    public static List<BooleanSolution> reduce(List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        long start = System.currentTimeMillis();
        long startGettingInteractions = System.currentTimeMillis();
        Set<Interaction> finalInteractions = getFinalInteractions(sample, t);
        long endGettingInteraction = System.currentTimeMillis();

        System.out.println(
                "Size contained Interactions calculated with the reduction approach:" + finalInteractions.size());

        int uniqueInteractions = 0;
        for (Interaction inter : finalInteractions) {
            if (inter.getCounter() == 1) {
                for (int i = 0; i < sample.size(); i++) {
                    BooleanSolution config = sample.get(i);
                    if ((config.get(Math.abs(inter.getContainedFeatures().get(0)) - 1)
                            == inter.getContainedFeatures().get(0))
                    //							&& (config.get(Math.abs(inter.getContainedFeatures().get(1)) - 1) == inter
                    //									.getContainedFeatures().get(1))
                    ) {
                        if (inter.getCounter() == 1) {
                            reducedSample.add(config);
                            sample.remove(config);
                            uniqueInteractions++;
                        }
                    }
                }
            }
        }
        System.out.println("Unique interactions in sample: " + uniqueInteractions);
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

        long end = System.currentTimeMillis();

        float secGettingInteractions = (endGettingInteraction - startGettingInteractions) / 1000F;
        System.out.println("Seconds needed to calculate the reduced sample: " + secGettingInteractions);

        float sec = (end - start) / 1000F;
        System.out.println("Seconds needed to calculate the reduced sample: " + sec);

        return reducedSample;
    }

    public static void revertReduction(List<BooleanSolution> reducedSample, int t) {
        for (int i = reducedSample.size() - 1; i > 0; i--) {
            reducedSample.remove(i);
            computeCoverage(reducedSample, t);
        }
    }

    private static void computeCoverage(List<BooleanSolution> reducedSample, int t) {
        // count how many interactions are in the sample overall
        long countAppearingInteractions = LexicographicIterator.<Void>stream(
                        t, reducedSample.get(0).size() * 2)
                .map(interaction -> {
                    int[] array = new int[2];
                    array[0] =
                            interaction.elementIndices[0] > reducedSample.get(0).size() - 1
                                    ? -(interaction.elementIndices[0]
                                            - reducedSample.get(0).size()
                                            + 1)
                                    : interaction.elementIndices[0] + 1;
                    //					array[1] = interaction.elementIndices[1] > reducedSample.get(0).size() - 1
                    //							? -(interaction.elementIndices[1] - reducedSample.get(0).size() + 1)
                    //							: interaction.elementIndices[1] + 1;
                    return array;
                })
                .filter(interaction -> {
                    for (BooleanSolution config : reducedSample) {
                        if (((interaction[0] < 0 && config.get(-interaction[0] - 1) == interaction[0])
                                || (interaction[0] > 0 && config.get(interaction[0] - 1) == interaction[0]))
                        //								&& ((interaction[1] < 0 && config.get(-interaction[1] - 1) == interaction[1])
                        //										|| (interaction[1] > 0 && config.get(interaction[1] - 1) == interaction[1]))
                        ) {
                            return true;
                        }
                    }
                    return false;
                })
                .count();
        System.out.println("Number of interactions in sample after reduction: " + countAppearingInteractions
                + " ; Size of sample: " + reducedSample.size());
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
        long start = System.currentTimeMillis();
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

        long end = System.currentTimeMillis();
        float sec = (end - start) / 1000F;
        System.out.println("Seconds needed to calculate the random reduced sample: " + sec);

        return reducedSample;
    }

    private static Set<Interaction> getFinalInteractions(List<BooleanSolution> sample, int t) {
        return LexicographicIterator.<Void>stream(t, sample.get(0).size() * 2)
                .map(interaction -> {
                    int[] array = new int[2];
                    array[0] = interaction.elementIndices[0] > sample.get(0).size() - 1
                            ? -(interaction.elementIndices[0] - sample.get(0).size() + 1)
                            : interaction.elementIndices[0] + 1;
                    //			array[1] = interaction.elementIndices[1] > sample.get(0).size() - 1
                    //					? -(interaction.elementIndices[1] - sample.get(0).size() + 1)
                    //					: interaction.elementIndices[1] + 1;
                    return array;
                })
                .map((int[] interaction) -> {
                    List<Integer> interactionList = new ArrayList<>();
                    interactionList.add(interaction[0]);
                    //			interactionList.add(interaction[1]);
                    //			Collections.sort(interactionList);
                    return new Interaction(interactionList);
                })
                .filter((Interaction interaction) -> {
                    for (BooleanSolution config : sample) {
                        int firstFeature = interaction.getContainedFeatures().get(0);
                        //				int secondFeature = interaction.getContainedFeatures().get(1);
                        if (((firstFeature < 0
                                        && config.get(-interaction
                                                                .getContainedFeatures()
                                                                .get(0)
                                                        - 1)
                                                == firstFeature)
                                || (firstFeature > 0 && config.get(firstFeature - 1) == firstFeature))
                        //						&& ((secondFeature < 0 && config.get(-secondFeature - 1) == secondFeature)
                        //								|| (secondFeature > 0 && config.get(secondFeature - 1) == secondFeature))
                        ) {
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
