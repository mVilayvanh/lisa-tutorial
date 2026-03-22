package it.unive.lisa.tutorial;

import java.io.File;

import org.junit.Test;

import it.unive.lisa.AnalysisException;
import it.unive.lisa.DefaultConfiguration;
import it.unive.lisa.LiSA;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.conf.LiSAConfiguration.GraphType;
import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;

public class IntervalsWithOverflowTest {

    @Test
    public void testIntervalsWithOverflowAnalysis() throws ParsingException, AnalysisException {
        File input = new File("inputs/intervalsoverflow.imp");
        System.out.println("INPUT EXISTS: " + input.exists());
        System.out.println("INPUT ABS PATH: " + input.getAbsolutePath());

        Program program = IMPFrontend.processFile("inputs/intervalsoverflow.imp");

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/intervalsoverflow";
        conf.analysisGraphs = GraphType.HTML;

        System.out.println("WORKDIR: " + new File(conf.workdir).getAbsolutePath());

        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new IntervalsWithOverflow()),
                DefaultConfiguration.defaultTypeDomain());

        LiSA lisa = new LiSA(conf);
        System.out.println("BEFORE LISA RUN");
        lisa.run(program);
        System.out.println("AFTER LISA RUN");

        File out = new File(conf.workdir);
        System.out.println("OUTPUT DIR EXISTS: " + out.exists());
        if (out.exists()) {
            File[] files = out.listFiles();
            if (files != null) {
                for (File f : files)
                    System.out.println("OUTPUT ENTRY: " + f.getName());
            }
        }
    }
}