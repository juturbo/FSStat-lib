package virtualthread.lib;

import java.util.Arrays;

public record FSReport(long totalFiles, long[] distribution) {

    public FSReport merge(FSReport other) {
        long newTotal = this.totalFiles + other.totalFiles;
        long[] newDistribution = new long[this.distribution.length];
        
        for (int i = 0; i < newDistribution.length; i++) {
            newDistribution[i] = this.distribution[i] + other.distribution[i];
        }
        
        return new FSReport(newTotal, newDistribution);
    }

    @Override
    public String toString() {
        return "FSReport{" +
            "totalFiles=" + totalFiles +
            ", distribution=" + Arrays.toString(distribution) +
            '}';
    }
}
