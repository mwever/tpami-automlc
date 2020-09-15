package benchmark.core.impl.optimizer.cfg.ggp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.epochx.gr.op.crossover.WhighamCrossover;
import org.epochx.gr.op.init.GrowInitialiser;
import org.epochx.gr.op.mutation.WhighamMutation;
import org.epochx.gr.representation.GRCandidateProgram;
import org.epochx.representation.CandidateProgram;
import org.epochx.tools.grammar.Grammar;
import org.epochx.tools.random.MersenneTwisterFast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import ai.libs.jaicore.basic.algorithm.AOptimizer;
import ai.libs.jaicore.components.model.ComponentInstance;
import benchmark.core.api.AListenable;
import benchmark.core.api.IOptimizer;
import benchmark.core.api.input.IPlanningOptimizationTask;
import benchmark.core.api.output.IOptimizationOutput;
import benchmark.core.api.output.IOptimizationSolutionCandidateFoundEvent;

public class GGP<M> extends AOptimizer<IPlanningOptimizationTask<M>, IOptimizationOutput<M>, Double> implements IOptimizer<IPlanningOptimizationTask<M>, M> {

	private static final Logger logger = LoggerFactory.getLogger(GGP.class);

	private MersenneTwisterFast rng = new MersenneTwisterFast(42);
	private final CFGConverter converter;
	private final IObjectEvaluator<ComponentInstance, Double> evaluator;

	private Map<String, Double> cacheMap = new HashMap<>();
	private Grammar grammar;
	private IOptimizationSolutionCandidateFoundEvent<M> bestSolution;

	class ResultListener {
		@Subscribe
		public void rcvEvent(final IOptimizationSolutionCandidateFoundEvent<M> e) {
			if (e.getScore() == null) {
				return;
			}

			if (GGP.this.updateBestSeenSolution(e.getOutput())) {
				GGP.this.bestSolution = e;
				System.out.println("Update best seen solution with score " + e.getScore() + " and candidate " + e.getSolutionCandidate());
			}
		}
	}

	public GGP(final IGeneticOptimizerConfig config, final IPlanningOptimizationTask<M> input) {
		super(config, input);
		this.converter = new CFGConverter(input.getComponents(), input.getRequestedInterface());
		this.evaluator = input.getDirectEvaluator(this.getClass().getSimpleName());
		if (this.evaluator instanceof AListenable) {
			((AListenable) this.evaluator).registerListener(new ResultListener());
		}
	}

	@Override
	public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
		switch (this.getState()) {
		case CREATED:
			System.out.println("Create");
			return this.activate();
		case ACTIVE:
			System.out.println("Active");
			Semaphore finished = new Semaphore(0);
			Thread evoThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String grammarString = GGP.this.converter.toGrammar();
						GGP.this.grammar = new Grammar(grammarString);
						// For initializing the population.
						GrowInitialiser initPop = new GrowInitialiser(GGP.this.rng, GGP.this.grammar, GGP.this.getConfig().getPopulationSize(), 50, false);
						List<CandidateProgram> population = new ArrayList<>(initPop.getInitialPopulation());
						Collections.sort(population);
						while (!Thread.currentThread().isInterrupted()) {
							System.out.println("Evaluate Population");
							try {
								GGP.this.evaluate(population);
							} catch (InterruptedException e) {
								System.out.println("Got interrupted");
								logger.info("Got interrupted. Shutdown task now.");
								finished.release();
								break;
							}

							List<CandidateProgram> offspring = new ArrayList<>(GGP.this.getConfig().getPopulationSize());
							// keep elite
							for (int i = 0; i < GGP.this.getConfig().getElitismSize(); i++) {
								offspring.add(population.get(i));
							}
							if (Thread.interrupted()) {
								System.out.println("Thread got interrupted, cancel GGP.");
								throw new InterruptedException();
							}

							// fill up offspring with recombinations
							while (offspring.size() < GGP.this.getConfig().getPopulationSize()) {
								if (Thread.interrupted()) {
									throw new InterruptedException();
								}

								CandidateProgram child1 = GGP.this.tournament(population).clone();
								CandidateProgram child2 = GGP.this.tournament(population).clone();

								double randomX = GGP.this.rng.nextDouble();
								if (randomX < GGP.this.getConfig().getCrossoverRate()) {
									WhighamCrossover xover = new WhighamCrossover(GGP.this.rng);
									CandidateProgram[] xoverprograms = xover.crossover(child1.clone(), child2.clone());
									if (xoverprograms != null) {
										child1 = xoverprograms[0];
										child2 = xoverprograms[1];
									}
								}

								child1 = GGP.this.mutate(child1);
								child2 = GGP.this.mutate(child2);

								offspring.add(child1);
								if (offspring.size() < GGP.this.getConfig().getPopulationSize()) {
									offspring.add(child2);
								}
							}
							population = offspring;
						}
					} catch (InterruptedException e) {
						System.err.println("GGP thread got interrupted, release semaphore and shutdown.");
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						finished.release();
					}
				}
			});
			evoThread.start();
			try {
				System.out.println("Wait for " + this.getInput().getGlobalTimeout().milliseconds() + "ms");
				boolean acquired = finished.tryAcquire(this.getInput().getGlobalTimeout().milliseconds(), TimeUnit.MILLISECONDS);
				if (!acquired) {
					System.err.println("Timeout occurred for evo thread. Now interrupt it.");
					evoThread.interrupt();
				}
			} catch (InterruptedException e) {
				System.err.println("Main GGP thread got interrupted, now interrupt evoThread.");
				evoThread.interrupt();
			}
			return this.terminate();
		default:
			throw new IllegalStateException("Illegal state for this algorithm to run anything.");
		}
	}

	private CandidateProgram mutate(final CandidateProgram program) {
		CandidateProgram mutated = program.clone();
		if (this.rng.nextDouble() < this.getConfig().getMutationRate()) {
			WhighamMutation mutation = new WhighamMutation(this.rng);
			mutated = mutation.mutate(mutated);
		}
		return mutated;
	}

	private CandidateProgram tournament(final List<CandidateProgram> population) {
		List<CandidateProgram> candidates = new ArrayList<CandidateProgram>(population);
		Collections.shuffle(candidates, new Random(this.rng.nextLong()));
		List<CandidateProgram> tournamentCandidates = IntStream.range(0, this.getConfig().getTournamentSize()).mapToObj(x -> candidates.get(x)).collect(Collectors.toList());
		Collections.sort(tournamentCandidates);
		return tournamentCandidates.get(0);
	}

	private void evaluate(final List<CandidateProgram> population) throws InterruptedException {
		ExecutorService pool = Executors.newFixedThreadPool(this.getConfig().cpus());
		IObjectEvaluator<ComponentInstance, Double> evaluator = this.getInput().getDirectEvaluator(this.getClass().getSimpleName());
		Semaphore semaphore = new Semaphore(0);
		System.out.println("evaluate " + population.size() + " candidates.");

		AtomicBoolean interrupted = new AtomicBoolean(false);

		for (CandidateProgram individual : population) {
			if (this.cacheMap.containsKey(individual.toString())) {
				((GRCandidateProgram) individual).setFitnessValue(this.cacheMap.get(individual.toString()));
				semaphore.release();
			} else {
				Runnable evaluateTask = new Runnable() {
					@Override
					public void run() {
						try {
							if (Thread.interrupted() || interrupted.get()) {
								semaphore.release();
							}
							ComponentInstance ci = GGP.this.converter.grammarStringToComponentInstance(individual.toString());
							GRCandidateProgram realInd = ((GRCandidateProgram) individual);
							try {
								realInd.setFitnessValue(evaluator.evaluate(ci));
							} catch (ObjectEvaluationFailedException | InterruptedException e) {
								realInd.setFitnessValue(10000.0);
							} finally {
								semaphore.release();
							}
						} catch (Throwable e) {
							System.err.println(individual.toString());
							e.printStackTrace();
							semaphore.release();
						}
					}
				};
				pool.submit(evaluateTask);
			}
		}
		try {
			semaphore.acquire(population.size());
		} catch (InterruptedException e) {
			interrupted.set(true);
			pool.shutdownNow();
			throw e;
		}
		population.stream().forEach(x -> {
			try {
				this.cacheMap.put(x.toString(), x.getFitness());
			} catch (Exception e) {
				// could not cache fitness => nullpointer exception must have occurred, so ignore the candidate and do not put it into the cache.
			}
		});
	}

	@Override
	public ComponentInstance getResultAsComponentInstance() {
		if (this.bestSolution == null) {
			return null;
		}
		return this.bestSolution.getSolutionCandidate();
	}

	@Override
	public M getResult() {
		if (this.bestSolution == null) {
			return null;
		}
		return this.bestSolution.getObject();
	}

	@Override
	public String getName() {
		return "ggp";
	}

	@Override
	public IGeneticOptimizerConfig getConfig() {
		return (IGeneticOptimizerConfig) super.getConfig();
	}

}
