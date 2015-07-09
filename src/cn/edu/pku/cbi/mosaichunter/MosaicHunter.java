package cn.edu.pku.cbi.mosaichunter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cn.edu.pku.cbi.mosaichunter.config.ConfigManager;
import cn.edu.pku.cbi.mosaichunter.config.MosaicHunterConfig;
import cn.edu.pku.cbi.mosaichunter.config.Parameter;
import cn.edu.pku.cbi.mosaichunter.log.LogOutputStream;

public class MosaicHunter {
    
    public static final String VERSION = "1.0";
    public static final String WEBSITE = "http://mosaichunter.cbi.pku.edu.cn";
    public static final String COMMAND = "java -jar mosaichunter.jar";
    public static final int HELP_WIDTH = 70;
    public static final int HELP_INDENT = 3;

    public static void main(String[] args) throws Exception {
        
        
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
                printHelpMessage(config, true);
                return;
            }
            if (args.length == 1) {
                printHelpMessage(config, false);
                return;
            }
        }

        initLog();
        printHead(args);

        ConfigManager.getInstance().print();
        
        BamScanner scanner = new BamScanner();
        scanner.scan();
        
        StatsManager.printStats();
        
    }
    
    private static void printHead(String[] args) {
        System.out.println("MosaicHunter " + VERSION);
        System.out.println(new Date());
        System.out.println("Parameters:");
        for (String a : args) {
            System.out.print(a + " ");
        }
        System.out.println();
        System.out.println();
    }
    
    private static void initLog() throws Exception {
        String logDir = ConfigManager.getInstance().get(null, "output_dir", ".");
        File logDirFile = new File(logDir);
        if (!logDirFile.exists()) {
            logDirFile.mkdirs();
        }
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
        String date = df.format(new Date());
        
        String stdoutLogFile = new File(logDir, "stdout_" + date + ".log").getAbsolutePath();
        LogOutputStream stdout = new LogOutputStream(stdoutLogFile, System.out);
        System.setOut(new PrintStream(stdout, true));
        
        String stderrLogFile = new File(logDir, "stderr_" + date + ".log").getAbsolutePath();
        LogOutputStream stderr = new LogOutputStream(stderrLogFile, System.err);
        System.setErr(new PrintStream(stderr, true));
        
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
        
        
        InputStream in = null;

        if (configFile == null || configFile.trim().isEmpty()) {
            configFile = cmd.getOptionValue("C");
            if (configFile != null && new File(configFile).isFile()) { 
                in = new FileInputStream(configFile);
            }
        } else {
            in = MosaicHunter.class.getClassLoader().getResourceAsStream(configFile);
        }
        if (in != null) {
            try {
                ConfigManager.getInstance().loadProperties(in); 
            } catch (IOException ioe) {
               System.out.println("invalid config file: " + configFile); 
               return false;
            } finally {
                in.close();
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
    
    private static void print() {
        System.out.println();
    }
    
    private static void print(Object message) {
        System.out.println(message);
    }
    
    private static void printf(Object message) {
        System.out.print(format(HELP_INDENT, HELP_WIDTH, message));
    }
    
    private static void printf(int indent, Object message) {
        System.out.print(format(indent, HELP_WIDTH, message));
    }
    
    private static void printVersion() {
        print("MosaciHunter v" + VERSION + 
              ", a post-zygotic single nucleotide mosaicism calling tool");
    }
    
    private static void printMoreInfo() {
        print("For more infomation, please visit " + WEBSITE);
    }
    
    private static void printHelpMessage() {
        printVersion();
        print("Usage: ");
        printf(COMMAND + " <configuration> <options>");
        print("where");
        printf("<configuration> could be 'genome', 'exome', 'exome_parameters', "
                + "or you can specify your own configuration file by '-C config_file'.");
        printf("Use '" + COMMAND + " help <configuration>' "
                + "for help of each configuration.");
        print();
        printMoreInfo();
    }
    
    private static void printHelpMessage(MosaicHunterConfig config, boolean details) {
        printVersion();
        print();
        if (config.getShortHelpMessages() != null) {
            for (String msg: config.getHelpMessages()) {
                printf(0, msg);
            }
        }
        print();
        print("Usage: ");
        printf(COMMAND + " " + config.getName() + " -P parameter=value -P ...");
        print();
        print("Options:");
        printParameters(config.getParameters());
        print();
        
        
        if (details) {
            printf(""); // TODO
        } else {
            printf("Use '" + COMMAND + " help " + config.getName() + "' "
                    + "for more details.");
        }
        print();
        printMoreInfo();

    }
    
    private static void printParameters(Parameter[] parameters) {
        for (Parameter p : parameters) {
            printf((p.getNamespace() != null ? p.getNamespace() + "." : "") + p.getName());
            StringBuilder sb = new StringBuilder();
            sb.append(p.getDescription());
            if (p.getDefaultValue() != null) {
                Object value = p.getDefaultValue();
                sb.append(" Default value is ");
                sb.append(value instanceof String ? "'" + value + "'" : value);
                sb.append(".");
            } else if (p.isRequired()) {
                sb.append(" Required.");
            }
            
            printf(HELP_INDENT * 2, sb.toString());
        }
    }
    
}
