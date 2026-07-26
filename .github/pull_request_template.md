## Summary

<!-- What performance or correctness question does this change address? -->

## Evidence

<!-- Link tests, benchmark results, JFR findings, or a decision record. -->

## Verification

```text
./mvnw clean verify
```

## Checklist

- [ ] Behavior is covered by focused tests.
- [ ] Competing implementations preserve the same semantics.
- [ ] Benchmark inputs and setup are outside the measured region where appropriate.
- [ ] Results are observable and run in forked JVMs.
- [ ] Performance claims include environment, uncertainty, and limitations.
- [ ] No secrets, raw customer data, hostnames, local paths, logs, or JFR recordings are committed.
- [ ] Documentation and changelog are updated.
