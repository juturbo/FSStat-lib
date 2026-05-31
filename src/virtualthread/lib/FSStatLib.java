package virtualthread.lib;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FSStatLib {

    public Future<FSReport> getFSReport(String d, long maxFS, int nb) {
        FutureTask<FSReport> rootTask = new FutureTask<>(() -> {
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                return computeDirectory(Path.of(d), maxFS, nb, executor);
            } // close() called implicitly
        });
        Thread.ofVirtual().start(rootTask);
        return rootTask;
    }

    private FSReport computeDirectory(Path dir, long maxFS, int nb, ExecutorService executor) {
        long totalFiles = 0;
        long[] distribution = new long[nb + 1];
        List<Future<FSReport>> subDirTasks = new ArrayList<>();
        double bandSize = (double) maxFS / nb;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    subDirTasks.add(executor.submit(() -> computeDirectory(entry, maxFS, nb, executor)));
                } else if (Files.isRegularFile(entry)) {
                    totalFiles++;

                    long size = Files.size(entry);
                    int bandIndex;

                    if (size > maxFS) {
                        bandIndex = nb; 
                    } else {
                        bandIndex = Math.min((int) (size / bandSize), nb - 1);
                    }
                    distribution[bandIndex]++;
                }
            }
        } catch (IOException e) {
            // skip directories that cannot be opened.
        }

        FSReport currentReport = new FSReport(totalFiles, distribution);

        for (Future<FSReport> task : subDirTasks) {
            try {
                FSReport subReport = task.get(); 
                currentReport = currentReport.merge(subReport);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error executing task: " + e.getMessage());
            }
        }

        return currentReport;
    }
}
