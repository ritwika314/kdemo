// Copyright (c) 2014-2016 K Team. All Rights Reserved.
package org.kframework.krun;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import com.beust.jcommander.JCommander;

public class KRunOptionsTest {

    @Test
    public void testOn() {
        KRunOptions options = new KRunOptions();
        new JCommander(options, "--statistics", "on", "--io", "on");
        assertTrue(options.experimental.statistics);
        assertTrue(options.io());
    }

    @Test
    public void testOff() {
        KRunOptions options = new KRunOptions();
        new JCommander(options, "--statistics", "off", "--io", "off");
        assertFalse(options.experimental.statistics);
        assertFalse(options.io());
    }

    @Test
    public void testSimulation() {
        KRunOptions options = new KRunOptions();
        new JCommander(options, "one", "--simulation", "--directory two three\\ four");
        assertEquals(Arrays.asList("--directory", "two", "three four"), options.experimental.simulation);
    }

}
