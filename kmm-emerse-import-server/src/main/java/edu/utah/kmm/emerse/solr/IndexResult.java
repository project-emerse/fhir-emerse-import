package edu.utah.kmm.emerse.solr;

/**
 * Tracks index successes and failures.
 */
public class IndexResult {

    private int succeeded;

    private int failed;

    public IndexResult success(boolean success) {
        int i = success ? ++succeeded : ++failed;
        return this;
    }

    public void combine(IndexResult result) {
        succeeded += result.succeeded;
        failed += result.failed;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public double getPercentageSucceeded() {
        return (double) succeeded / (double) (succeeded + failed) * 100.0;
    }
}
