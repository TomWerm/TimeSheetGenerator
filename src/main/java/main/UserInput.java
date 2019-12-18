package main;

import i18n.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.JFileChooser;

import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

public class UserInput {

  private CommandLine commandLine;
  private final String[] args;
  
  private String currentDirectory = null; //caches the last used directory for the file chooser
  
  public UserInput(String[] args) {
    this.args = args;
  }
  
  public Request parse() throws ParseException {
      CommandLineParser dp = new DefaultParser();
      this.commandLine = dp.parse(UserInputOption.getOptions(), args, false);
      
      if (commandLine.hasOption(UserInputOption.HELP.getOption().getOpt())) {
          return Request.HELP;
      }
      if (commandLine.hasOption(UserInputOption.VERSION.getOption().getOpt())) {
          return Request.VERSION;
      }
      
      /*
       * Checks whether gui or file option were used.
       * Using none of them makes it impossible to get the files.
       */
      if (commandLine.hasOption(UserInputOption.GUI.getOption().getOpt())
          && commandLine.hasOption(UserInputOption.FILE.getOption().getOpt())) {
        throw new ParseException(ResourceHandler.getMessage("guiAndFileError"));
      } else {
          return Request.GENERATE;
      }
  }
  
  public void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("TimeSheetGenerator", UserInputOption.getOptions());
  }
  
  public void printVersion() {
      String version = null;
      String buildTime = null;
      String branch = null;
      String commit = null;
      
      try {
          InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("project.properties");
          
          if (inputStream != null) {
              Properties properties = new Properties();
              properties.load(inputStream);
              
              String[] buildProperties = properties.getProperty("buildInfo").split(";");
              if (buildProperties.length >= 4) {
                  version = buildProperties[0];
                  buildTime = buildProperties[1];
                  
                  branch = buildProperties[2];
                  if (branch.startsWith("$")) {
                      branch = null;
                  }
                  commit = buildProperties[3];
                  if (commit.startsWith("$")) {
                      commit = null;
                  }
              }
          }
      } catch (IOException e) {}
      
      if (version == null) { // general error
          System.out.println(ResourceHandler.getMessage("versionNotFoundError"));
      } else {
          System.out.println(ResourceHandler.getMessage("version", version));
          
          // false when buildnumber-maven-plugin not executed (possible when run from ide)
          if (buildTime != null && branch != null && commit != null) {
              System.out.println();
              System.out.println(ResourceHandler.getMessage("buildInfo", branch, commit, buildTime));
          }
      }
  }
  
  public File getFile(UserInputFile userInputFile) throws IOException {
    File file = null;
    
    if (commandLine.hasOption(UserInputOption.FILE.getOption().getOpt())) {
        String[] fileArgs = commandLine.getOptionValues(UserInputOption.FILE.getOption().getOpt());
        
        //TODO Get rid of the magic numbers. Maybe put them into UserInputFile?
        assert(fileArgs.length == 3); //Because ParseException is thrown if there are less args
        switch (userInputFile) {
          case JSON_GLOBAL:
            file = new File(fileArgs[0]);
            break;
          case JSON_MONTH:
            file = new File(fileArgs[1]);
            break;
          case OUTPUT:
            file = new File(fileArgs[2]);
            break;
          default:
            break;
        }
    } else {
      JFileChooser fileChooser = new JFileChooser();
      
      if (currentDirectory == null || currentDirectory.isEmpty()) {
    	  fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
      } else {
    	  fileChooser.setCurrentDirectory(new File(currentDirectory));
      }
      
      //Configure the accept behavior of the JFileChooser
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setFileFilter(userInputFile.getFileFilter());
      fileChooser.setAcceptAllFileFilterUsed(false);
      
      //Cosmetic changes to title
      fileChooser.setDialogTitle(userInputFile.getDialogTitel());
      
      //Get allowed file extensions
      String[] exts = userInputFile.getFileFilter().getExtensions();
      
      //Show dialog and check for errors
      switch (userInputFile.getFileOperation()) {
      	case OPEN:
    	  if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
    		  throw new IOException(ResourceHandler.getMessage("fileOpenError"));
    	  }
    	  file = fileChooser.getSelectedFile();
    	  
    	  if (!file.exists()) {
    		  throw new IOException(ResourceHandler.getMessage("fileNotExistError"));
    	  } else if (exts.length > 0 && !Arrays.asList(exts).contains(FilenameUtils.getExtension(file.getName()))) {
    		  throw new IOException(ResourceHandler.getMessage("unsupportedExtensionError"));
    	  }
    	  break;
      	case SAVE:
    	  if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
    		  throw new IOException(ResourceHandler.getMessage("saveError"));
    	  }
    	  file = fileChooser.getSelectedFile();
    	  
    	  if (exts.length > 0 && !Arrays.asList(exts).contains(FilenameUtils.getExtension(file.getName()))) {
    		  String ext = exts[0].startsWith(".") ? exts[0] : "." + exts[0];
    		  file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ext);
    	  }
    	  break;
    	default: //Never used, since OPEN and SAVE are the only enum values
    	  break;
      }
      
      currentDirectory = file.getParent();
    }
    
    return file;
  }

  public enum Request {
      HELP,
      VERSION,
      GENERATE;
  }
}
