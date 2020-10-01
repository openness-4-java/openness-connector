package it.unimore.dipi.iot.openness.utils;

import it.unimore.dipi.iot.openness.exception.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 30/09/2020 - 21:34
 */
public class LinuxCliExecutor implements CommandLineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LinuxCliExecutor.class);

    public int executeCommand(String command) throws CommandLineException {

        try{

            logger.info("Executing command: {}", command);

            Runtime runtime = Runtime.getRuntime();
            Process pr = runtime.exec(command);

            pr.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line = "";

            logger.debug("############ Cli Command Line Result #############");

            while((line = bufferedReader.readLine()) != null)
                logger.debug(line);

            logger.debug("############ Cli Command Line Result #############");

            logger.info("Command Result: {}", pr.exitValue());

            return pr.exitValue();

        }catch (Exception e){
            throw new CommandLineException(String.format("Error Executing Command: %s Error: %s", command, e.getLocalizedMessage()));
        }

    }

    public static void main(String[] args) {

        try{

            String command = "keytool -list -keystore example.client.chain.p12 -storepass changeit";

            CommandLineExecutor commandLineExecutor = new LinuxCliExecutor();
            commandLineExecutor.executeCommand(command);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
