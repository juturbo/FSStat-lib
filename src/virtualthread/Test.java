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

        var startTime = System.currentTimeMillis();
        Future<FSReport> reportFuture = lib.getFSReport(targetDir, maxFileSize, numberOfBands);

        FSReport report = reportFuture.get();
        var elapsedTime = System.currentTimeMillis() - startTime;

        System.out.println("Report generated in " + elapsedTime + " ms:");
        System.out.println("Total files: " + report.totalFiles());
        long bandSize = maxFileSize / numberOfBands;
        for (int i = 0; i < numberOfBands; i++) {
            long lowerBound = i * bandSize;
            long upperBoundExclusive = (i + 1) * bandSize;
            System.out.printf("Band %d %s: %d files%n", i, formatBandRange(lowerBound, upperBoundExclusive), report.distribution()[i]);
        }
        System.out.printf("Band %d [%s, +inf): %d files%n", numberOfBands, formatSize(maxFileSize), report.distribution()[numberOfBands]);
    }

    private static String formatBandRange(long lowerBound, long upperBoundExclusive) {
        return "[" + formatSize(lowerBound) + ", " + formatSize(upperBoundExclusive) + ")";
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
