# Contributing

Contributions are welcome when they preserve the repository's
correctness-first measurement discipline.

## Setup

Use a full JDK from version 21 through 25:

```bash
./mvnw clean verify
```

The wrapper supplies Maven. Spotless applies Google Java Format:

```bash
./mvnw spotless:apply
```

## Change workflow

1. Define the behavior or performance question.
2. Add or update correctness tests first.
3. Keep benchmark setup outside the measured region.
4. Return or consume computed results.
5. Run the full Maven reactor.
6. Exercise relevant JMH or JFR commands.
7. Document evidence, trade-offs, and limits.

For a new candidate implementation, add it behind an existing or new behavioral
interface and prove equivalence with differential tests. Do not optimize by
accepting fewer inputs, skipping validation, changing overflow behavior, or
weakening output semantics.

## Benchmark checklist

- use JMH rather than manual wall-clock loops
- use at least one fork for smoke tests and multiple forks for evidence
- provide explicit warm-up and measurement iterations
- source variable inputs from `@Param` or state
- exclude fixture generation unless it is the behavior under investigation
- retain result observability
- inspect allocation/GC metrics alongside the primary score
- never turn shared-runner timing into a CI threshold

## Publishing measurements

Committed evidence must include:

- benchmark commit
- CPU, memory, operating system, JVM vendor/version, GC, and heap flags
- JMH version, forks, threads, warm-up, and measurement settings
- raw machine-readable results
- interpretation and limitations

Normalize local launcher paths before committing. Do not commit hostnames,
usernames, `.jfr` files, console logs, profiler dumps, secrets, or real customer
data.

## Pull requests

Use the pull-request template. Keep changes focused and use imperative commit
subjects. Include verification commands and explain why the evidence supports
the conclusion.
