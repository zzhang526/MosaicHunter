package cn.edu.pku.cbi.mosaichunter;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;

public class MosaicHunter {
   
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[] {"-C", "conf/default.properties"};
        }
        
        if (!loadConfiguration(args)) {
            return;
        }
        
        BamScanner scanner = new BamScanner();
        scanner.scan();
        
        StatsManager.printStats();
        
        // TODO
        /*
           for (int i = 0; i < MosaicFilter.cnt.length; ++i) {
         
            long a = MosaicFilter.cnt[i];
            if (a > 0) {
                System.out.println(i + " " + a);
            }
        }
*/
        
    }
    
    private static boolean loadConfiguration(String[] args) throws Exception {
        Option helpOption = new Option( "help", "print this message" );

        OptionBuilder.withArgName("file");
        OptionBuilder.hasArg();
        OptionBuilder.withDescription("config file");
        OptionBuilder.withLongOpt("config");
        Option configFileOption = OptionBuilder.create("C");
        
        OptionBuilder.withArgName("property=value");
        OptionBuilder.hasArgs(2);
        OptionBuilder.withValueSeparator();
        OptionBuilder.withDescription("properties that overrides the ones in config file");
        OptionBuilder.withLongOpt("properties");
        Option propertiesOption = OptionBuilder.create("P");
        
        Options options = new Options();
        options.addOption(helpOption);
        options.addOption(configFileOption);
        options.addOption(propertiesOption);
     
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("TODO", options, true);
                return false;
            }
        } catch (ParseException pe) {
            System.out.println(pe.getMessage());
            return false;
        }
        
        String configFile = cmd.getOptionValue("C");
        if (configFile != null) {
            try {
                ConfigManager.getInstance().loadProperties(configFile); 
            } catch (IOException ioe) {
               System.out.println("invalid config file: " + configFile); 
            }
        }
        
        Properties properties = cmd.getOptionProperties("P");
        if (properties != null) {
            ConfigManager.getInstance().putAll(properties);
        }
        
        ConfigManager.getInstance().print();
        return true;
    }
}
