package com.qroll.report;

import java.io.IOException;
import java.nio.file.Path;

public class ReportGenerator {
    public void export(Reportable report , Path outputPath) throws IOException
    {
        report.export(outputPath);
        System.out.println("Report exported to: " + outputPath.toAbsolutePath());
    }
    }



