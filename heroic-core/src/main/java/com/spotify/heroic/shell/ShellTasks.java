package com.spotify.heroic.shell;

import java.util.List;
import java.util.SortedMap;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.google.common.base.Joiner;

import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ShellTasks {
    public static final Joiner joiner = Joiner.on(", ");

    final SortedMap<String, ShellTask> tasks;
    final AsyncFramework async;

    public AsyncFuture<Void> evaluate(List<String> command, ShellIO io) throws Exception {
        if (command.isEmpty()) {
            return async.failed(new Exception("Empty command"));
        }

        final String taskName = command.iterator().next();
        final List<String> args = command.subList(1, command.size());

        final ShellTask task;

        try {
            task = resolveTask(taskName);
        } catch(final Exception e) {
            return async.failed(e);
        }

        final TaskParameters params = task.params();

        if (params != null) {
            final CmdLineParser parser = new CmdLineParser(params);

            try {
                parser.parseArgument(args);
            } catch (CmdLineException e) {
                return async.failed(e);
            }

            if (params.help()) {
                parser.printUsage(io.out(), null);
                return async.resolved();
            }
        }

        try {
            return task.run(io, params);
        } catch(Exception e) {
            return async.failed(e);
        }
    }

    ShellTask resolveTask(final String taskName) {
        final SortedMap<String, ShellTask> selected = tasks.subMap(taskName, taskName
                + Character.MAX_VALUE);

        final ShellTask exact;

        // exact match
        if ((exact = selected.get(taskName)) != null) {
            return exact;
        }

        // no fuzzy matches
        if (selected.isEmpty()) {
            throw new IllegalArgumentException(String.format("No task matching (%s)", taskName));
        }

        if (selected.size() > 1) {
            throw new IllegalArgumentException(String.format("Too many (%d) matching tasks (%s)", selected.size(),
                    joiner.join(selected.keySet())));
        }

        return selected.values().iterator().next();
    }
}