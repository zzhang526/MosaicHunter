package cn.edu.pku.cbi.mosaichunter;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.config.MosaicHunterConfig;

public class MosaicHunter {
    
    public static final String VERSION = "1.0";
    public static final String WEBSITE = "http://cbi.pku.edu.cn";

    
    public static void main(String[] args) throws Exception {
        
               
       //printf(0,3, "1 12 123 1234 12345 123456 1234567 12345678 123456789 1234567890 1 12 123 1234 12345 123456 1234567 12345678 123456789 1234567890 ");

        //printf(0,3, "       1        1         ");
        
        //        args = new String[] {"-C", "conf/exome.properties"};

        //args = new String[] {""};
        //args = new String[] {"genome"};
        
        String cmd1 = args.length > 0 ? getCommand(args[0]) : null;
        String cmd2 = args.length > 1 ? getCommand(args[1]) : null;
        if (isHelp(cmd1)) {
            String tmp = cmd1;
            cmd1 = cmd2;
            cmd2 = tmp;
        }
        
        MosaicHunterConfig config = MosaicHunterConfig.getConfig(cmd1);
        if (args.length == 0 || 
            (config == null && isHelp(cmd2))) {
            printHelpMessage();
            return;
        }
        if (!loadConfiguration(args, config == null ? null : config.getConfigFile())) {
            printHelpMessage();
            return;
        }
        if (config != null) {
            if (isHelp(cmd2)) {
                printHelpMessage(config);
            } else {
                printShortHelpMessage(config);
            }
            return;
        }
        
        System.out.println("run");
        if (true) return;

        
        
        ConfigManager.getInstance().print();
        
      
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
    
    private static boolean loadConfiguration(String[] args, String configFile) throws Exception {
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
        options.addOption(configFileOption);
        options.addOption(propertiesOption);
     
        CommandLineParser parser = new BasicParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException pe) {
            System.out.println(pe.getMessage());
            return false;
        }
        
        if (configFile == null || configFile.trim().isEmpty()) {
            configFile = cmd.getOptionValue("C");
        }
        if (configFile != null) {
            try {
                ConfigManager.getInstance().loadProperties(configFile); 
            } catch (IOException ioe) {
               System.out.println("invalid config file: " + configFile); 
               return false;
            }
        }
        
        Properties properties = cmd.getOptionProperties("P");
        if (properties != null) {
            ConfigManager.getInstance().putAll(properties);
        }
        return !ConfigManager.getInstance().getProperties().isEmpty();
    }
    
    private static boolean isHelp(String cmd) {
        return cmd != null && (cmd.equalsIgnoreCase("help") || cmd.equalsIgnoreCase("h"));
    }
    
    private static String getCommand(String cmd) {
        if (cmd == null) {
            return "";
        }
        cmd = cmd.trim();
        if (cmd.startsWith("-")) {
            cmd = cmd.substring(1);
        }
        if (cmd.startsWith("-")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return null;
        }
        return cmd;
    }
    
    private static String format(int indent, int width, Object message) {
        if (indent < 0) {
            indent = 0;
        }
        if (width <= indent) {
            width = indent + 1;
        }
        width -= indent;
        
        String space = "";
        for (int i = 0; i < indent; ++i) {
            space += ' ';
        }
        
        String text = String.valueOf(message);
        
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        for (;;) {
            while (pos < text.length() && text.charAt(pos) == ' ') {
                pos++;
            }
            int newPos = pos + width;

            if (newPos >= text.length()) {
                newPos = text.length();
            } else {
                while (newPos > pos && text.charAt(newPos) != ' ') {
                    newPos--;
                }
                if (newPos <= pos) {
                    newPos = pos + width;
                }
            }
            String line = text.substring(pos, newPos).trim();
            if (!line.isEmpty()) {
                sb.append(space + line + "\n");
            }
            if (newPos >= text.length()) {
                break;
            }
            pos = newPos;
        }
        return sb.toString();
    }
    
    private static void print(Object message) {
        System.out.println(message);
    }
    
    private static void print(int indent, String message) {
        for (int i = 0; i < indent; ++i) {
            System.out.print(' ');
        }
        System.out.println(message);
    }
    
    private static void printf(int width, Object message) {
        System.out.print(format(0, width, message));
    }
    
    private static void printf(int indent, int width, String message) {
        System.out.print(format(indent, width, message));
    }
    
    private static void printVersion() {
        print("12345678901234567890223456789032345678904234567890523456789062345678971234567890");
        print("MosaciHunter v" + VERSION + 
              ", a post-zygotic single nucleotide mosaicism calling tool");
    }
    
    private static void printMoreInfo() {
        print("For more infomation, please visit " + WEBSITE);
        print("12345678901234567890223456789032345678904234567890523456789062345678971234567890");
    }
    
    private static void printHelpMessage() {
        printVersion();
        print("Usage: ");
        print(3, "java -jar mosaichunter.jar <configuration> <options>");
        print("where");
        printf(3, 70, "<configuration> could be 'genome', 'excome', 'exome_parameters', "
                + "'paired', or you can specify your own configuration file by '-C config_file'.");
        printf(3, 70, "Use 'java -jar mosaichunter.jar help <configuration>' "
                + "for help of each configuration.");
        print("");
        printMoreInfo();
    }
    
    private static void printHelpMessage(MosaicHunterConfig config) {
        System.out.println("help message for config: " + config.getName() + ", including parameters and filters, blahblahblah...");
        
    }
    
   
    
    private static void printShortHelpMessage(MosaicHunterConfig config) {
        printVersion();
        
        
        System.out.println("short help message for config: " + config.getName() + ", including parameters and filters, blahblahblah...");
        
    }
    
}
