package top.ilovemyhome.dagtask.agent.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.ilovemyhome.dagtask.si.TaskExecution;
import top.ilovemyhome.dagtask.si.TaskInput;
import top.ilovemyhome.dagtask.si.TaskOutput;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * CPU-intensive task that computes prime numbers up to a given bound.
 *
 * <p>Supports two algorithms:
 * <ul>
 *   <li>{@code sieve} — Sieve of Eratosthenes, O(n log log n)</li>
 *   <li>{@code trial} — Trial division, O(n sqrt(n)), heavier CPU load</li>
 * </ul>
 *
 * <p>The {@code iterations} parameter repeats the computation to extend
 * execution duration and increase CPU utilization.</p>
 *
 * <p>Example input JSON:
 * <pre>{"upperBound":1000000,"algorithm":"sieve","iterations":10}</pre>
 */
public class CpuIntensiveExecution implements TaskExecution {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public TaskOutput doExecute(TaskInput input) {
        Long taskId = input.taskId();
        Param param = input.getInputAs(Param.class);

        logger.info("Start CPU-intensive task taskId={}, param={}", taskId, param);

        if (Objects.isNull(param) || param.upperBound() < 2 || param.iterations() < 1) {
            throw new IllegalArgumentException(
                "Invalid param. Required: upperBound >= 2, iterations >= 1");
        }

        long totalPrimes = 0;
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= param.iterations(); i++) {
            if (Thread.currentThread().isInterrupted()) {
                logger.warn("TaskId={} interrupted at iteration {}/{}",
                    taskId, i, param.iterations());
                throw new IllegalStateException("Task interrupted");
            }
            List<Integer> primes = switch (param.algorithm()) {
                case "sieve" -> sieveOfEratosthenes(param.upperBound());
                case "trial" -> trialDivisionPrimes(param.upperBound());
                default -> throw new IllegalArgumentException(
                    "Unknown algorithm: " + param.algorithm() + ". Use 'sieve' or 'trial'.");
            };
            totalPrimes += primes.size();

            logger.info("Iteration {}/{} completed, primes found: {}",
                i, param.iterations(), primes.size());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("TaskId={} completed in {}ms, total primes computed: {}",
            taskId, duration, totalPrimes);

        return TaskOutput.success(taskId,
            "primes=" + totalPrimes + ",durationMs=" + duration);
    }

    /**
     * Sieve of Eratosthenes — efficient prime computation.
     *
     * @param n upper bound (inclusive)
     * @return list of primes up to n
     */
    private List<Integer> sieveOfEratosthenes(int n) {
        BitSet isComposite = new BitSet(n + 1);
        List<Integer> primes = new ArrayList<>();

        for (int i = 2; i <= n; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Task interrupted during sieve computation");
            }
            if (!isComposite.get(i)) {
                primes.add(i);
                if ((long) i * i <= n) {
                    for (int j = i * i; j <= n; j += i) {
                        isComposite.set(j);
                    }
                }
            }
        }
        return primes;
    }

    /**
     * Trial division — intentionally less efficient, higher CPU load.
     *
     * @param n upper bound (inclusive)
     * @return list of primes up to n
     */
    private List<Integer> trialDivisionPrimes(int n) {
        List<Integer> primes = new ArrayList<>();
        for (int candidate = 2; candidate <= n; candidate++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new IllegalStateException("Task interrupted during trial division computation");
            }
            boolean isPrime = true;
            int limit = (int) Math.sqrt(candidate);
            for (int divisor = 2; divisor <= limit; divisor++) {
                if (candidate % divisor == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primes.add(candidate);
            }
        }
        return primes;
    }

    /**
     * Input parameter DTO for CpuIntensiveExecution.
     *
     * @param upperBound  maximum number to test for primality (>= 2)
     * @param algorithm   "sieve" or "trial"
     * @param iterations  how many times to repeat the computation (>= 1)
     */
    public record Param(int upperBound, String algorithm, int iterations) {
        public Param {
            if (algorithm == null || algorithm.isBlank()) {
                algorithm = "sieve";
            }
            if (iterations < 1) {
                iterations = 1;
            }
            if (upperBound < 2) {
                upperBound = 2;
            }
        }
    }
}
