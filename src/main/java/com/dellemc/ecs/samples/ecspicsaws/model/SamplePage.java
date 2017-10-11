package com.dellemc.ecs.samples.ecspicsaws.model;

/**
 * Holds the model for the MainController's page that lists the samples available.
 */
public class SamplePage {
    private String sampleName;
    private String sampleDescription;
    private String samplePath;

    public SamplePage(String sampleName, String sampleDescription, String samplePath) {
        this.sampleName = sampleName;
        this.sampleDescription = sampleDescription;
        this.samplePath = samplePath;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getSampleDescription() {
        return sampleDescription;
    }

    public String getSamplePath() {
        return samplePath;
    }

}
