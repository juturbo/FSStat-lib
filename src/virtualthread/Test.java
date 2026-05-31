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

        Future<FSReport> reportFuture = lib.getFSReport(targetDir, maxFileSize, numberOfBands);

        System.out.println("Analysis in progress in background. Executing other operations in the main thread...");

        FSReport report = reportFuture.get(); 

        System.out.println("\n=== REPORT COMPLETED ===");
        System.out.println("Total files: " + report.totalFiles());
        for (int i = 0; i < numberOfBands; i++) {
            System.out.printf("Band %d: %d files%n", i, report.distribution()[i]);
        }
        System.out.printf("Files > MaxFS: %d files%n", report.distribution()[numberOfBands]);
        System.out.println(report.toString());
    }
}
