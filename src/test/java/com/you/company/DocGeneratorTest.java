package com.you.company;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DocGeneratorTest {
    private DocGenerator docGenerator;

    @BeforeEach
    public void setUp() throws Exception {
        docGenerator = new DocGenerator();
    }

    @Test
    @DisplayName("test getResult() method")
    public void testGetResult() {
        assertTrue((new DocGenerator()).generateWordFile("tsql"));
        assertTrue((new DocGenerator()).generateWordFile("php"));
        assertTrue((new DocGenerator()).generateWordFile("objc"));
        assertTrue((new DocGenerator()).generateWordFile("js"));
        assertTrue((new DocGenerator()).generateWordFile("java"));
        assertTrue((new DocGenerator()).generateWordFile("web"));
        assertTrue((new DocGenerator()).generateWordFile("cs"));
        assertTrue((new DocGenerator()).generateWordFile("cpp"));
        assertTrue((new DocGenerator()).generateWordFile("c"));
        assertTrue((new DocGenerator()).generateWordFile("ts"));
    }

}
