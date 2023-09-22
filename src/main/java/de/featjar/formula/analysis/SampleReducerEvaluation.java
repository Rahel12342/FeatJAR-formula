package de.featjar.formula.analysis;

import de.featjar.formula.analysis.bool.BooleanSolution;
import de.featjar.formula.analysis.combinations.LexicographicIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SampleReducerEvaluation {

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
    public static List<BooleanSolution> reduce(Map<String, Integer> features, List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();

        Set<InteractionEvaluation> finalInteractions = getFinalInteractions(sample, t);

        System.out.println("Size contained Interactions calculated from Rahel:" + finalInteractions.size());
        BooleanSolution bestConfig = new BooleanSolution();

        int counter = 1;

        while (true) {

            Map<BooleanSolution, Double> configsWithScore = new HashMap<>();
            for (BooleanSolution config : sample) {
                configsWithScore.put(config, 0.0);
            }

            double bestScore = 0.0;

            // walk through all configs and give them a score and find the best scored
            // config
            m:
            for (BooleanSolution config : sample) {
                List<Integer> configAsList = new ArrayList<>();
                {
                    for (int n : config.get()) {
                        configAsList.add(n);
                    }
                }

                for (InteractionEvaluation inter : finalInteractions) {
                    if (configAsList.containsAll(inter.getContainedFeatures())) {
                        if (inter.getCounter() == 1) {
                            bestConfig = config;
                            bestScore = 100;
                            break m;
                        } else {
                            configsWithScore.replace(
                                    config, configsWithScore.get(config) + (1.0 / (double) inter.getCounter()));
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

            // remove interactions that are now covered from the interations that still need
            // to be covered
            List<Integer> configAsList = new ArrayList<>();
            {
                for (int n : bestConfig.get()) {
                    configAsList.add(n);
                }
            }

            for (Iterator<InteractionEvaluation> iterator = finalInteractions.iterator(); iterator.hasNext(); ) {
                InteractionEvaluation inter = iterator.next();
                if (configAsList.containsAll(inter.getContainedFeatures())) {
                    iterator.remove();
                }
            }

            //			System.out.println(counter + ". Durchlauf; größe der übrigen Interaktionen: " +
            // finalInteractions.size()
            //					+ "; Größe der übrigen Konfigs: " + sample.size() + "Größe des finalen samples: "
            //					+ reducedSample.size());
            counter++;
            if (finalInteractions.isEmpty()) {
                break;
            }
        }

        return reducedSample;
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
    public static List<BooleanSolution> reduceRandom(
            Map<String, Integer> features, List<BooleanSolution> sample, int t) {

        List<BooleanSolution> reducedSample = new ArrayList<BooleanSolution>();
        Set<InteractionEvaluation> finalInteractions = getFinalInteractions(sample, t);

        for (Iterator<BooleanSolution> iterator = sample.iterator(); iterator.hasNext(); ) {
            BooleanSolution config = iterator.next();
            int interactionsBeforeAdding =
                    getFinalInteractions(reducedSample, t).size();
            reducedSample.add(config);
            if (interactionsBeforeAdding
                    == getFinalInteractions(reducedSample, t).size()) {
                reducedSample.remove(config);
            } else {
                List<Integer> configAsList = new ArrayList<>();
                {
                    for (int n : config.get()) {
                        configAsList.add(n);
                    }
                }
                for (Iterator<InteractionEvaluation> iterator2 = finalInteractions.iterator(); iterator2.hasNext(); ) {
                    InteractionEvaluation inter = iterator2.next();
                    if (configAsList.containsAll(inter.getContainedFeatures())) {
                        iterator.remove();
                    }
                }
            }

            if (finalInteractions.isEmpty()) {
                break;
            }
        }

        return reducedSample;
    }

    private static Set<InteractionEvaluation> getFinalInteractions(List<BooleanSolution> sample, int t) {
        return LexicographicIterator.<Void>stream(t, sample.get(0).size() * 2)
                .map(interaction -> {
                    int[] array = new int[2];
                    array[0] = interaction.elementIndices[0] > sample.get(0).size() - 1
                            ? -(interaction.elementIndices[0] - sample.get(0).size() + 1)
                            : interaction.elementIndices[0] + 1;
                    array[1] = interaction.elementIndices[1] > sample.get(0).size() - 1
                            ? -(interaction.elementIndices[1] - sample.get(0).size() + 1)
                            : interaction.elementIndices[1] + 1;
                    return array;
                })
                .map((int[] interaction) -> {
                    List<Integer> interactionList = new ArrayList<>();
                    interactionList.add(interaction[0]);
                    interactionList.add(interaction[1]);
                    Collections.sort(interactionList);
                    return new InteractionEvaluation(interactionList);
                })
                .filter((InteractionEvaluation interaction) -> {
                    for (BooleanSolution config : sample) {
                        int firstFeature = interaction.getContainedFeatures().get(0);
                        int secondFeature = interaction.getContainedFeatures().get(1);
                        if (((firstFeature < 0
                                                && config.get(-interaction
                                                                        .getContainedFeatures()
                                                                        .get(0)
                                                                - 1)
                                                        == firstFeature)
                                        || (firstFeature > 0 && config.get(firstFeature - 1) == firstFeature))
                                && ((secondFeature < 0 && config.get(-secondFeature - 1) == secondFeature)
                                        || (secondFeature > 0 && config.get(secondFeature - 1) == secondFeature))) {
                            interaction.increaseCounter();
                        }
                    }

                    // TODO: valide? Solver aufrufen! dann ist es noch ne extra interaction (um am
                    // ende coverage raus zu finden)
                    return interaction.getCounter() > 0;
                })
                .collect(Collectors.toSet());
    }
}

class InteractionEvaluation {

    private List<Integer> containedFeatures;
    private int counter = 0;

    public InteractionEvaluation(List<Integer> containedFeatures) {
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
