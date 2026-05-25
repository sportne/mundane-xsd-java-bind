package io.github.mundanej.mxjb.generator.core;

import io.github.mundanej.mxjb.generator.api.GeneratorRequest;
import io.github.mundanej.mxjb.generator.api.GeneratorResult;
import java.util.ArrayList;
import java.util.List;

/** Benchmark-only access to generator phase timings without changing the public generator API. */
public final class CoreGeneratorTimingProbe {
  private CoreGeneratorTimingProbe() {}

  public static TimedGeneration generate(GeneratorRequest request) {
    List<PhaseTiming> phaseTimings = new ArrayList<>();
    GeneratorResult result =
        new CoreGenerator()
            .generate(
                request,
                (phaseName, elapsedNanos) ->
                    phaseTimings.add(new PhaseTiming(phaseName, elapsedNanos)));
    return new TimedGeneration(result, List.copyOf(phaseTimings));
  }

  public record TimedGeneration(GeneratorResult result, List<PhaseTiming> phaseTimings) {
    public TimedGeneration {
      phaseTimings = List.copyOf(phaseTimings);
    }

    @Override
    public List<PhaseTiming> phaseTimings() {
      return List.copyOf(phaseTimings);
    }
  }

  public record PhaseTiming(String phaseName, long elapsedNanos) {}
}
