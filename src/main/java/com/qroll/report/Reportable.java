package com.qroll.report;
import java.io.IOException;
import java.nio.file.Path;


public interface Reportable {
    void export(Path outputPath) throws IOException ;
    String getDefaultFileName();
}
