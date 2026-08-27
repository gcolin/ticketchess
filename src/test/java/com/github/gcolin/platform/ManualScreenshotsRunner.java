package com.github.gcolin.platform;

public final class ManualScreenshotsRunner {

    private ManualScreenshotsRunner() {}

    public static void main(String[] args) throws Exception {
        try {
            ManualScreenshotsTest test = new ManualScreenshotsTest();
            test.setup();
            test.generateManualScreenshots();
            System.out.println("SCREENSHOTS_DONE");
            System.exit(0);
        } catch (Throwable err) {
            err.printStackTrace();
            System.exit(1);
        }
    }
}
