package it.unimore.dipi.iot.openness.utils;

import it.unimore.dipi.iot.openness.exception.CommandLineException;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 01/10/2020 - 09:04
 */
public interface CommandLineExecutor {

    public int executeCommand(String command) throws CommandLineException;

}
