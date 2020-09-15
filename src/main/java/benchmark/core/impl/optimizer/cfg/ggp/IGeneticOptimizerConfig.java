package benchmark.core.impl.optimizer.cfg.ggp;

import benchmark.core.api.input.IOptimizerConfig;

public interface IGeneticOptimizerConfig extends IOptimizerConfig {

	public static final String K_TOURNAMENT_SIZE = "ga.tournamentsize";
	public static final String K_POPULATION_SIZE = "ga.population.size";
	public static final String K_ELITISM_SIZE = "ga.elitism.size";
	public static final String K_MUTATION_RATE = "ga.mutation.rate";
	public static final String K_CROSSOVER_RATE = "ga.crossover.rate";
	public static final String K_MAX_DEPTH = "ga.crossover.rate";

	@Key(K_TOURNAMENT_SIZE)
	@DefaultValue("2")
	public int getTournamentSize();

	@Key(K_POPULATION_SIZE)
	@DefaultValue("15")
	public int getPopulationSize();

	@Key(K_ELITISM_SIZE)
	@DefaultValue("1")
	public int getElitismSize();

	@Key(K_MUTATION_RATE)
	@DefaultValue("0.1")
	public double getMutationRate();

	@Key(K_CROSSOVER_RATE)
	@DefaultValue("0.9")
	public double getCrossoverRate();

	@Key(K_MAX_DEPTH)
	@DefaultValue("50")
	public int getMaxDepth();

}
