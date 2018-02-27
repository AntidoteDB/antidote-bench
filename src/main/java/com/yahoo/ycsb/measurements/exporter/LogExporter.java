package com.yahoo.ycsb.measurements.exporter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class LogExporter implements MeasurementsExporter
{

    private static final Logger log = LogManager.getLogger(LogExporter.class);

    @Override
    public void write(String metric, String measurement, int i) throws IOException
    {
        log.info("[" + metric + "], " + measurement + ", " + i + "\n");
    }

    @Override
    public void write(String metric, String measurement, double d) throws IOException
    {
        log.info("[" + metric + "], " + measurement + ", " + d + "\n");
    }

    @Override
    public void close() throws IOException
    {

    }
}
