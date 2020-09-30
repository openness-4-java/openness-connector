package it.unimore.dipi.iot.openness.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project openness-connector
 * @created 30/09/2020 - 21:34
 */
public class ExecuteLinuxShellCommand {

    public static void main(String[] args) {

        try{

            String command = "keytool -list -keystore example.client.chain.p12 -storepass changeit";

            Runtime runtime = Runtime.getRuntime();
            Process pr = runtime.exec(command);

            pr.waitFor();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pr.getInputStream()));

            String line = "";

            while((line = bufferedReader.readLine()) != null){
                System.out.println(line);
            }

        }catch (Exception e){
            e.printStackTrace();
        }


    }

}
