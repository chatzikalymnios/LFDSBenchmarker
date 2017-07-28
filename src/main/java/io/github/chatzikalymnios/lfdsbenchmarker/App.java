package io.github.chatzikalymnios.lfdsbenchmarker;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.chatzikalymnios.lfds.EliminationBackoffStack;
import io.github.chatzikalymnios.lfds.LockFreeStack;

public class App {
	private static final Logger logger = LoggerFactory.getLogger(App.class);

	/* Data structure names */
	private static final String EB_STACK = "EBStack";

	/* Default values */
	private static final int DEFAULT_SPIN_DELAY = 100000; // nanoseconds

	/* Command line options */
	private static final Option HELP_OPTION = new Option("h", false, "print this message");
	private static final Option NUM_THREADS_OPTION = Option.builder("t").argName("num").hasArg()
			.desc("number of threads to use").required().build();
	private static final Option DATA_STRUCTURE_OPTION = Option.builder("d").argName("datastructure").hasArg()
			.desc("data structure to benchmark\n[" + EB_STACK + "]").required().build();
	private static final Option NUM_ITEMS_OPTION = Option.builder("i").argName("num").hasArg()
			.desc("number of items to insert and remove").required().build();
	private static final Option WORKLOAD_OPTION = Option.builder("w").argName("microseconds").hasArg()
			.desc("concurrent workload in microseconds").required().build();
	private static final Option SPIN_DELAY_OPTION = Option.builder("s").argName("nanoseconds").hasArg().desc(
			"amount of time to wait for elimination partner in nanoseconds (applicable to EBStack) [default: 100000]")
			.build();

	private static final Options allOptions = new Options();

	static {
		allOptions.addOption(HELP_OPTION);
		allOptions.addOption(NUM_THREADS_OPTION);
		allOptions.addOption(DATA_STRUCTURE_OPTION);
		allOptions.addOption(NUM_ITEMS_OPTION);
		allOptions.addOption(WORKLOAD_OPTION);
		allOptions.addOption(SPIN_DELAY_OPTION);
	}

	private void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("benchmarker", allOptions);
	}

	private boolean checkHelpOption(String[] args) {
		return Arrays.asList(args).contains("-h");
	}

	public void run(String[] args) {
		if (checkHelpOption(args)) {
			printHelp();
			return;
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;

		try {
			line = parser.parse(allOptions, args);
		} catch (ParseException e) {
			logger.error("Command line parsing failed: " + e.getMessage());
			printHelp();
			System.exit(1);
		}

		String dataStructure = line.getOptionValue(DATA_STRUCTURE_OPTION.getOpt());

		// Number of threads option
		int numThreads = 0;

		try {
			numThreads = Integer.parseInt(line.getOptionValue(NUM_THREADS_OPTION.getOpt()));
		} catch (NumberFormatException e) {
			logger.error("Invalid number of threads: " + line.getOptionValue(NUM_THREADS_OPTION.getOpt()));
			System.exit(1);
		}

		if (numThreads < 1) {
			logger.error("Invalid number of threads: " + numThreads);
			System.exit(1);
		}

		// Number of items option
		int numItems = 0;

		try {
			numItems = Integer.parseInt(line.getOptionValue(NUM_ITEMS_OPTION.getOpt()));
		} catch (NumberFormatException e) {
			logger.error("Invalid number of items: " + line.getOptionValue(NUM_ITEMS_OPTION.getOpt()));
			System.exit(1);
		}

		if (numItems < 0) {
			logger.error("Invalid number of items: " + numItems);
			System.exit(1);
		}

		// Workload option
		int workload = 0;

		try {
			workload = Integer.parseInt(line.getOptionValue(WORKLOAD_OPTION.getOpt()));
		} catch (NumberFormatException e) {
			logger.error("Invalid workload: " + line.getOptionValue(WORKLOAD_OPTION.getOpt()));
			System.exit(1);
		}

		if (workload < 0) {
			logger.error("Invalid workload: " + workload);
			System.exit(1);
		}

		// Spin delay option
		int spinDelay = DEFAULT_SPIN_DELAY;

		if (line.hasOption(SPIN_DELAY_OPTION.getOpt())) {
			try {
				spinDelay = Integer.parseInt(line.getOptionValue(SPIN_DELAY_OPTION.getOpt()));
			} catch (NumberFormatException e) {
				logger.error("Invalid spin: " + line.getOptionValue(SPIN_DELAY_OPTION.getOpt()));
				System.exit(1);
			}
		}

		if (spinDelay < 0) {
			logger.error("Invalid spin: " + workload);
			System.exit(1);
		}

		Benchmark benchmark = null;

		switch (dataStructure) {
		case EB_STACK:
			LockFreeStack<Integer> stack = new EliminationBackoffStack<>(numThreads, spinDelay);
			benchmark = new StackBenchmark(stack, numThreads, numItems, workload);
			break;
		default:
			logger.error("Unknown data structure: " + dataStructure);
			System.exit(1);
		}

		try {
			benchmark.run();
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Benchmark ended with an exception: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new App().run(args);
	}

}
