// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.ktest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.kframework.ktest.CmdArgs.KTestOptions;
import org.kframework.ktest.StringMatcher.MatchFailure;
import org.kframework.utils.ColorUtil;
import org.kframework.utils.OS;
import org.kframework.utils.StringUtil;
import org.kframework.utils.errorsystem.KExceptionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A unified process class for kompile, kompile --pdf and krun processes.
 */
public class Proc<T> implements Runnable {

    /**
     * Global KTest state.
     */
    private final KTestOptions options;

    /**
     * Operating system program and arguments.
     */
    private final String[] args;

    /**
     * Directory to spawn process from.
     */
    private final File workingDir;

    /**
     * Expected program output with location information for output file.
     */
    private final Annotated<String, File> expectedOut;

    /**
     * Expected program error.
     */
    private final Annotated<String, File> expectedErr;

    /**
     * Process' input source.
     */
    private final File inputFile;

    /**
     * Input string to pass to program.
     */
    private final String procInput;

    /**
     * Comparator to compare program outputs with expected outputs.
     */
    private final StringMatcher strComparator;

    /**
     * Some arbitrary user data to later get with `getObj()'.
     */
    private final T obj;

    /**
     * Output file to be generated/overwritten when --update-out is used.
     */
    private final File outputFile;

    /**
     * Output file to be generated when --generate-out is used.
     */
    private final File newOutputFile;

    private final boolean warnings2errors;

    /**
     * Whether the process succeeded or not.
     */
    private boolean success;

    /**
     * Reason of failure. null if process is not started yet or it's succeeded.
     */
    private String reason = null;

    /**
     * How long did process take.
     */
    private long timeDelta = 0;

    /**
     * Output of the process.
     */
    private ProcOutput procOutput = new ProcOutput(null, null, -1, false);

    private final KExceptionManager kem;
    private final Map<String, String> env;

    /**
     *
     * @param obj this is basically an arbitrary object to keep in a process,
     *            this used to know which TestCase a process is running
     * @param args arguments to pass to process
     * @param inputFile process' input source -- used for logging
     * @param procInput null or empty string to not pass anything to program input
     * @param expectedOut null to ignore the program output, output messages are ignored when
     *                    program fails with an error
     * @param expectedErr null if not testing for error output, error messages are ignored when
     *                    program returns 0
     * @param strComparator comparator object to compare program outputs with expected outputs
     * @param outputFile output file to be updated when --update-out is used
     * @param newOutputFile output file to generated when --generate-out is used
     */
    public Proc(T obj, String[] args, File inputFile, String procInput,
                Annotated<String, File> expectedOut, Annotated<String, File> expectedErr,
                StringMatcher strComparator, File workingDir, KTestOptions options,
                File outputFile, File newOutputFile, KExceptionManager kem, Map<String, String> env, boolean warnings2errors) {
        this.obj = obj;
        this.args = args;
        this.inputFile = inputFile;
        this.procInput = procInput;
        this.expectedOut = expectedOut;
        this.expectedErr = expectedErr;
        this.strComparator = strComparator;
        this.workingDir = workingDir;
        this.options = options;
        this.outputFile = outputFile;
        this.newOutputFile = newOutputFile;
        this.kem = kem;
        this.env = new HashMap<>(env);
        success = options.dry;
        this.warnings2errors = warnings2errors;
        // add the /bin directory to the PATH if not there already
        String binDir = ExecNames.getBinDirectory();
        // windows is case insensitive w.r.t environment variables, so first find the exact match
        String path = this.env.keySet().stream().filter(key -> key.toUpperCase().equals("PATH")).findFirst().orElseGet(() -> "PATH");
        if (this.env.containsKey(path)) {
            String allPaths = this.env.get(path);
            if (!allPaths.contains(binDir))
                this.env.put(path, allPaths + File.pathSeparator + binDir);
        } else {
            this.env.put(path, binDir);
        }
    }

    public Proc(T obj, String[] args, File workingDir, KTestOptions options, KExceptionManager kem, Map<String, String> env, boolean warnings2errors) {
        this(obj, args, null, "", null, null, options.getDefaultStringMatcher(), workingDir,
                options, null, null, kem, env, warnings2errors);
    }

    @Override
    public void run() {
        // pass the --warnings-to-errors flag to all processes if specified to ktest
        String[] warningsArgs = args;
        if (warnings2errors) {
            warningsArgs = Arrays.copyOf(args, args.length + 1);
            warningsArgs[args.length] = "--warnings-to-errors";
        }
        if (options.getDebug()) {
            String[] debugArgs = Arrays.copyOf(warningsArgs, warningsArgs.length + 1);
            debugArgs[warningsArgs.length] = "--debug";
            if (expectedErr != null) {
                // We want to use --debug and compare error outputs too. In this case we need to
                // make two runs:
                // 1) We pass --debug and collect output with stack trace.
                // 2) We don't pass --debug and use output for comparison.
                ProcOutput debugOutput = runProc(debugArgs);
                procOutput = runProc(warningsArgs);
                if (!options.dry) {
                    handlePgmResult(procOutput, debugOutput);
                }
            } else {
                // Make one run with --debug
                procOutput = runProc(debugArgs);
                if (!options.dry) {
                    handlePgmResult(procOutput, null);
                }
            }
        } else {
            procOutput = runProc(warningsArgs);
            if (!options.dry) {
                handlePgmResult(procOutput, null);
            }
        }
    }

    private ProcOutput runProc(String[] args) {
        if (options.dry) {
            StringBuilder dryStr = new StringBuilder();
            dryStr.append(toLogString(args));
            if (options.getUpdateOut() && outputFile != null)
                dryStr.append(" >").append(outputFile);
            else if (options.getGenerateOut() && newOutputFile != null)
                dryStr.append(" >").append(newOutputFile);
            if (inputFile != null)
                dryStr.append(" <").append(inputFile);
            System.out.println(dryStr.toString());
            return null;
        } else {
            ProcessBuilder pb = new ProcessBuilder(args).directory(workingDir);
            pb.environment().clear();
            pb.environment().putAll(env);
            pb.environment().put("kompile", ExecNames.getKompile());
            pb.environment().put("krun", ExecNames.getKrun());
            pb.environment().put("kast", ExecNames.getKast());

            try {
                printRunningMsg(toLogString(args));
                long startTime = System.currentTimeMillis();
                Process proc = pb.start();

                // I'm using a different naming convention because this is more intuitive for me
                InputStream errorStream = proc.getErrorStream(); // program's error stream
                InputStream outStream = proc.getInputStream(); // program's output stream
                OutputStream inStream = proc.getOutputStream(); // program's input stream

                // pass input to process
                IOUtils.write(procInput, inStream);
                inStream.close();

                // asynchronously read outputs
                final ExecutorService service = Executors.newFixedThreadPool(2);
                final Future<String> outputGobbler = service.submit(new StreamGobbler(outStream));
                final Future<String> errorGobbler = service.submit(new StreamGobbler(errorStream));

                ProcStatus procStatus = wait(proc);
                timeDelta += System.currentTimeMillis() - startTime;

                try {
                    return new ProcOutput(outputGobbler.get(), errorGobbler.get(),
                            procStatus.returnCode, procStatus.timeout);
                } catch (ExecutionException e) {
                    // program was killed before producing output,
                    // set pgmOut and pgmErr null manually, in case one of the outputs is produced
                    // but other is not (not sure if that's possible, just to make sure..)
                    return new ProcOutput(null, null, procStatus.returnCode, procStatus.timeout);
                }
            } catch (IOException | InterruptedException e) {
                kem.registerInternalWarning(e.getMessage(), e);
                reportErr("program failed with exception: " + e.getMessage());
            }
            return null; // unreachable
        }
    }

    /**
     * @return true if process finished with success.
     */
    public boolean isSuccess() {
        return success;
    }

    public T getObj() {
        return obj;
    }

    /**
     * @return reason of failure. null when process is not started yet or it's succeeded.
     */
    public String getReason() {
        return reason;
    }

    public long getTimeDelta() {
        return timeDelta;
    }

    /**
     * @return Program output. null when proces is not started yet or it's failed before
     *         producing output (i.e. when killed because of timeout)
     */
    public String getPgmOut() {
        return procOutput.stdout;
    }

    /**
     * @return Program error output. null when proces is not started yet or it's failed before
     *         producing error output (i.e. when killed because of timeout)
     */
    public String getPgmErr() {
        return procOutput.stderr;
    }

    public static String toLogString(String[] args) {
        if (OS.current() == OS.WINDOWS) {
            return StringUtils.join(args, ' ');
        }
        return StringUtil.escapeShell(args, OS.current());
    }

    private ProcStatus wait(final Process proc) throws InterruptedException {
        final AtomicBoolean timeout = new AtomicBoolean(false);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                proc.destroy();
                timeout.set(true);
            }
        }, options.getTimeout());
        int ret = proc.waitFor();
        timer.cancel();
        return new ProcStatus(ret, timeout.get());
    }

    /**
     * Compare expected outputs with program outputs, set `reason' and `success' variables,
     * print information messages.
     */
    private void handlePgmResult(ProcOutput normalOutput, ProcOutput debugOutput) {
        String red = ColorUtil.RgbToAnsi(
                "red", options.getColorSetting(), options.getTerminalColor());
        String logStr = toLogString(args);
        if (normalOutput.returnCode == 0) {

            boolean doGenerateOut = false;

            // program ended successfully ..
            if (expectedOut == null) {
                // we're not comparing outputs
                success = true;
                System.out.format("Done with [%s] (time %d ms)%n", logStr, timeDelta);
                doGenerateOut = true;
            } else {
                try {
                    strComparator.matches(expectedOut.getObj(), normalOutput.stdout);

                    // outputs match
                    success = true;
                    System.out.format("Done with [%s] (time %d ms)%n", logStr, timeDelta);
                } catch (MatchFailure e) {
                    // outputs don't match
                    System.out.format(
                        "%sERROR: [%s] output doesn't match with expected output " +
                        "%s (time: %d ms)%s%n",
                        red, logStr, expectedOut.getAnn(), timeDelta, ColorUtil.ANSI_NORMAL);
                    reportOutMatch(e.getMessage());
                    System.out.println(getReason());
                    doGenerateOut = true;
                }
            }

            // https://github.com/kframework/k/wiki/Manual-(to-be-processed)#ktest-automatic-output-generation
            if (options.getUpdateOut() && outputFile != null) {
                System.out.println("Updating output file: " + outputFile);
                try {
                    IOUtils.write(normalOutput.stdout, new FileOutputStream(outputFile));
                } catch (IOException e) {
                    kem.registerInternalWarning(e.getMessage(), e);
                }
            }
            if (doGenerateOut && options.getGenerateOut() && newOutputFile != null) {
                System.out.println("Generating output file: " + newOutputFile);
                try {
                    IOUtils.write(normalOutput.stdout, new FileOutputStream(newOutputFile));
                } catch (IOException e) {
                    kem.registerInternalWarning(e.getMessage(), e);
                }
            }

        } else if (normalOutput.timeout) {

            System.out.format("%sERROR: [%s] killed due to timeout.%s%n",
                    red, logStr, ColorUtil.ANSI_NORMAL);
            reportTimeout();

        } else {

            // program ended with error ..
            if (expectedErr == null) {
                // we're not comparing error outputs
                System.out.format("%sERROR: [%s] failed with error code %d (time: %d ms)%s%n",
                        red, logStr, normalOutput.returnCode, timeDelta, ColorUtil.ANSI_NORMAL);
                if (debugOutput != null) {
                    System.out.format("error was: %s%n", debugOutput.stderr);
                } else {
                    System.out.format("error was: %s%n", normalOutput.stderr);
                }
                reportErr(normalOutput.stderr);
            } else {
                try {
                    strComparator.matches(expectedErr.getObj(), normalOutput.stderr);
                    // error outputs match
                    success = true;
                    System.out.format("Done with [%s] (time %d ms)%n", logStr, timeDelta);
                } catch (MatchFailure e) {
                    // error outputs don't match
                    System.out.format(
                        "%sERROR: [%s] throwed error, but expected error message doesn't match " +
                        "%s (time: %d ms)%s%n",
                        red, logStr, expectedErr.getAnn(), timeDelta, ColorUtil.ANSI_NORMAL);
                    if (debugOutput != null) {
                        System.out.format("error output was (except the stack trace): %s%n",
                                debugOutput.stderr);
                    } else {
                        System.out.format("error output was: %s%n", normalOutput.stderr);
                    }
                    reportErrMatch(e.getMessage());
                }
            }
        }
    }

    private void reportErr(String err) {
        assert reason == null;
        reason = err;
    }

    private void reportErrMatch(String message) {
        assert reason == null;
        reason = "Unexpected program error:\n" + message;
    }

    private void reportOutMatch(String message) {
        assert reason == null;
        reason = "Unexpected program output:\n" + message;
    }

    private void reportTimeout() {
        assert reason == null;
        reason = "Timeout";
    }

    private void printRunningMsg(String logStr) {
        StringBuilder b = new StringBuilder();
        b.append("Running [");
        b.append(logStr);
        b.append("]");
        if (inputFile != null) {
            b.append(" [input: ");
            b.append(inputFile);
            b.append("]");
        }
        if (outputFile != null) {
            b.append(" [output: ");
            b.append(outputFile);
            b.append("]");
        }
        System.out.println(b);
    }
}

class ProcOutput {
    public final String stdout;
    public final String stderr;
    public final int returnCode;
    public final boolean timeout;

    public ProcOutput(String stdout, String stderr, int returnCode, boolean timeout) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.returnCode = returnCode;
        this.timeout = timeout;
    }
}

class ProcStatus {
    public final int returnCode;
    public final boolean timeout;

    public ProcStatus(int returnCode, boolean timeout) {
        this.returnCode = returnCode;
        this.timeout = timeout;
    }
}

class StreamGobbler implements Callable<String> {
    InputStream is;

    StreamGobbler(InputStream is) {
        this.is = is;
    }

    public String call() throws IOException {
        return IOUtils.toString(is);
    }
}
