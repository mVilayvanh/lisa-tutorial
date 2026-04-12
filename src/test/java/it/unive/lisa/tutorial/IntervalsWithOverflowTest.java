package it.unive.lisa.tutorial;

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
        Program program = IMPFrontend.processFile("inputs/intervalsoverflow.imp");

        LiSAConfiguration conf = new DefaultConfiguration();
        conf.workdir = "outputs/intervalsoverflow";
        conf.analysisGraphs = GraphType.HTML;

        conf.abstractState = DefaultConfiguration.simpleState(
                DefaultConfiguration.defaultHeapDomain(),
                new ValueEnvironment<>(new IntervalsWithOverflow()),
                DefaultConfiguration.defaultTypeDomain());

        LiSA lisa = new LiSA(conf);
        lisa.run(program);
    }
}