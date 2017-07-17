package io.github.chatzikalymnios.lfdsbenchmarker;

import java.util.concurrent.ExecutionException;

public interface Benchmark {
    void run() throws InterruptedException, ExecutionException;
}
