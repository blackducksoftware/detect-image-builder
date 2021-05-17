/*
 * detect-image-builder
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.imagebuilder.utilities;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.executable.ExecutableOutput;
import com.synopsys.integration.executable.ExecutableRunnerException;
import com.synopsys.integration.executable.ProcessBuilderRunner;
import com.synopsys.integration.log.Slf4jIntLogger;

public class RunUtils {
    private String shell = "bash";

    private Logger logger = LoggerFactory.getLogger(getClass());

    public void runScript(String pathToScript, Map<String, String> args) {
        if (pathToScript == null) {
            return;
        }

        List<String> command = new LinkedList<>();
        command.add(shell);
        command.add(pathToScript);
        for (Map.Entry<String, String> keyValue : args.entrySet()) {
            command.add(keyValue.getKey());
            command.add(keyValue.getValue());
        }

        try {
            Slf4jIntLogger intLogger = new Slf4jIntLogger(logger);
            ProcessBuilderRunner pbr = new ProcessBuilderRunner(intLogger);
            ProcessBuilder pb = new ProcessBuilder();

            pb.command(command);

            ExecutableOutput output = pbr.execute(pb);
            if (output.getReturnCode() != 0) {
                logger.warn(String.format("Standard output from command %s: %s", command.toString(), output.getStandardOutput()));
                logger.warn(String.format("Error output from command %s: %s", command.toString(), output.getErrorOutput()));
            }
        } catch (ExecutableRunnerException e) {
            logger.warn(String.format("Execution of script %s failed.", pathToScript));
        }
    }
}
