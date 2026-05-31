package virtualthread;

import java.util.concurrent.Future;

import virtualthread.lib.FSReport;
import virtualthread.lib.FSStatLib;

public class Test {
    private static final long KB = 1024; // 1 KB
    private static final long MB = KB * 1024; // 1 MB

    public static void main(String[] args) throws Exception {
        FSStatLib lib = new FSStatLib();
        
        String targetDir = "."; 
        long maxFileSize = 1 * MB; // 1 MB
        int numberOfBands = 4;      

        System.out.println("Starting analysis...");
        var startTime = System.currentTimeMillis();
        Future<FSReport> reportFuture = lib.getFSReport(targetDir, maxFileSize, numberOfBands);
        System.out.println("Analysis in progress in background.\n");

        FSReport report = reportFuture.get();
        var elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("Report generated in " + elapsedTime + " ms:");
        System.out.println("Total files: " + report.totalFiles());
        long bandSize = maxFileSize / numberOfBands;
        for (int i = 0; i < numberOfBands; i++) {
            long lowerBound = i * bandSize;
            long upperBound = (i == numberOfBands - 1) ? maxFileSize : ((i + 1) * bandSize) - 1;
            System.out.printf(
                "Band %d [%s - %s]: %d files%n",
                i,
                formatSize(lowerBound),
                formatSize(upperBound),
                report.distribution()[i]
            );
        }
        System.out.printf("Files > %s: %d files%n", formatSize(maxFileSize), report.distribution()[numberOfBands]);
    }

    private static String formatSize(long bytes) {
        if (bytes >= MB && bytes % MB == 0) {
            return (bytes / MB) + " MB";
        }
        if (bytes >= KB && bytes % KB == 0) {
            return (bytes / KB) + " KB";
        }
        return bytes + " B";
    }
}
