/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package filemanagerGUI;

import LibraryLB.FileManaging.AutoBackupMaker;
import LibraryLB.FileManaging.FileReader;
import LibraryLB.Log;
import LibraryLB.Containers.ParametersMap;
import filemanagerGUI.dialog.CommandWindowController;
import filemanagerLogic.Enums;
import filemanagerLogic.fileStructure.ExtFile;
import filemanagerLogic.fileStructure.ExtFolder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import utility.ErrorReport;
import utility.FavouriteLink;
import utility.PathStringCommands;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB extends Application {
    public static final String HOME_DIR  = System.getProperty("user.dir")+File.separator;
    public static final String VIRTUAL_FOLDERS_DIR = HOME_DIR+"VIRTUAL_FOLDERS"+File.separator;
    public static final String ARTIFICIAL_ROOT = HOME_DIR+"ARTIFICIAL_ROOT";
    public static String ROOT_NAME = "ROOT";
    public static ExtFolder ArtificialRoot = new ExtFolder(ARTIFICIAL_ROOT);
    public static ExtFolder VirtualFolders = new ExtFolder(VIRTUAL_FOLDERS_DIR);
    public static FileLock GlobalLock;
    public static ObservableList<FavouriteLink> links;
    public static ObservableList<ErrorReport> errorLog;
    public static int DEPTH;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(false);
    public static int LogBackupCount;
    public static ParametersMap parameters;
    public static PathStringCommands customPath = new PathStringCommands(HOME_DIR);
    @Override
    public void start(Stage primaryStage) {
        
        Platform.runLater(()->{
            
            links = FXCollections.observableArrayList();
            errorLog = FXCollections.observableArrayList();
            ArtificialRoot.setPopulated(true);
            ArtificialRoot.setIsAbsoluteRoot(true);
            VirtualFolders.setPopulated(true);
            VirtualFolders.populateFolder();
            ArrayDeque<String> list = new ArrayDeque<>();
            try{
                //Log.changeStream('f', new File(DIR+"Log.txt"));
                list = new ArrayDeque(FileReader.readFromFile(HOME_DIR+"Parameters.txt","//","/*","*/"));
            }
            catch(Exception e){
                ErrorReport.report(e);                
            }
                parameters = new ParametersMap(list);
                Log.writeln("Parameters",parameters);
                DEBUG.set((boolean) parameters.defaultGet("debug",false));
                DEPTH = (int) parameters.defaultGet("lookDepth",2);
                LogBackupCount = (int) parameters.defaultGet("logBackupCount", 2);
                ROOT_NAME = (String) parameters.defaultGet("ROOT_NAME", ROOT_NAME);
                PathStringCommands.number = (String) parameters.defaultGet("filter.number", "#");
                PathStringCommands.fileName = (String) parameters.defaultGet("filter.name", "<n>");
                PathStringCommands.nameNoExt = (String) parameters.defaultGet("filter.nameNoExtension", "<nne>");
                PathStringCommands.filePath = (String) parameters.defaultGet("filter.path", "<ap>");
                PathStringCommands.extension = (String) parameters.defaultGet("filter.nameExtension", "<ne>");
                PathStringCommands.parent1 = (String) parameters.defaultGet("filter.parent1", "<p1>");
                PathStringCommands.parent2 = (String) parameters.defaultGet("filter.parent2", "<p2>");
                PathStringCommands.custom = (String) parameters.defaultGet("filter.custom", "<c>");
                PathStringCommands.relativeCustom = (String) parameters.defaultGet("filter.relativeCustom", "<rc>");
                CommandWindowController.truncateAfter = (Integer) FileManagerLB.parameters.defaultGet("code.truncateAfter", 100000);
                CommandWindowController.maxExecutablesAtOnce = (Integer) FileManagerLB.parameters.defaultGet("code.maxExecutables", 2);
                CommandWindowController.commandGenerate = (String) FileManagerLB.parameters.defaultGet("code.commandGenerate", "generate");
                CommandWindowController.commandApply = (String) FileManagerLB.parameters.defaultGet("code.commandApply", "apply");
                CommandWindowController.commandCreateVirtual = (String) FileManagerLB.parameters.defaultGet("code.createVirtualFolder", "virtual");
                CommandWindowController.commandListVirtualFolders = (String) FileManagerLB.parameters.defaultGet("code.listVirtualFolders", "listVirtualFolders");
                CommandWindowController.commandListVirtual = (String) FileManagerLB.parameters.defaultGet("code.listVirtual", "listVirtual");
                CommandWindowController.commandClear = (String) FileManagerLB.parameters.defaultGet("code.clear", "clear");
                CommandWindowController.commandAddToVirtual = (String) FileManagerLB.parameters.defaultGet("code.addToVirtual", "add");
                CommandWindowController.commandList = (String) FileManagerLB.parameters.defaultGet("code.list", "list");
                CommandWindowController.commandListRec = (String) FileManagerLB.parameters.defaultGet("code.listRec", "listRec");
                CommandWindowController.commandSetCustom = (String) FileManagerLB.parameters.defaultGet("code.setCustom", "setCustom");
                CommandWindowController.commandHelp = (String) FileManagerLB.parameters.defaultGet("code.help", "help");
            try{
                for(ExtFile f:VirtualFolders.getFilesCollection()){
                    Files.deleteIfExists(f.toPath());
                }
                Files.deleteIfExists(VirtualFolders.toPath());
                Files.createDirectory(VirtualFolders.toPath());
                Files.deleteIfExists(ArtificialRoot.toPath());
                Files.createFile(ArtificialRoot.toPath());
                
                GlobalLock = new RandomAccessFile(ArtificialRoot,"rw").getChannel().tryLock();
                
                Log.writeln("Locked "+GlobalLock.toString());

            }catch(Exception e){
                
                ErrorReport.report(e);
            }
            ArtificialRoot.propertyName.set(ROOT_NAME);
            links.add(new FavouriteLink(ROOT_NAME,""));
            ViewManager.getInstance().newWindow(ArtificialRoot, ArtificialRoot);
            ViewManager.getInstance().updateAllWindows();
        });
        if(DEBUG.not().get()){
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        }
        
    } 
    public static void main(String[] args) {
        launch(args);
    }
    public static void remount(){
        
        File[] roots = File.listRoots();
        for(ExtFile f:ArtificialRoot.getFilesCollection()){
            if(!Files.isDirectory(f.toPath())){
                ArtificialRoot.files.remove(f.propertyName.get());
            }
        }
        for (File root : roots) {
            //Log.writeln("Root["+i+"]:" + roots[i].getAbsolutePath());
            mountDevice(root.getAbsolutePath());
        }
    }
    public static boolean mountDevice(String name){
        boolean result = false;
        name = name.toUpperCase();
        //Log.write("Mount:",name);
        Path path = Paths.get(name);
        if(Files.isDirectory(path)){
            ExtFolder device = new ExtFolder(name);
            int nameCount = path.getNameCount();
            //Log.write("Is direcory");
            if(nameCount == 0){
                result = true;
                String newName = path.getRoot().toString();
                //Log.writeln("newName= "+newName);
                device.propertyName.set(newName);
                if(!ArtificialRoot.files.containsKey(newName)){
                    ArtificialRoot.files.put(newName, device);
                    device.update();
                    
                }else{
                    result = false;
                }
            }
        }
        return result;
    }
    public static boolean folderIsVirtual(ExtFile fileToCheck){
        ExtFolder baseFolder = FileManagerLB.VirtualFolders;
        HashSet<String> set = new HashSet<>();
        for(ExtFile file:baseFolder.files.values()){
            set.add(file.getAbsoluteDirectory());
        }
        return set.contains(fileToCheck.getAbsoluteDirectory());
    }
    public static Set<String> getRootSet(){
        HashSet<String> set = new HashSet<>();
        for(ExtFile file:ArtificialRoot.files.values()){
            set.add(file.propertyName.get());
        }
        return set;
        
    }
    public static void doOnExit(){
        try {
            GlobalLock.release();
            GlobalLock.channel().close();
            LibraryLB.FileManaging.FileReader.writeToFile(HOME_DIR+"Log.txt", Log.getInstance().list);
            VirtualFolders.populateFolder();
            for(ExtFile f:VirtualFolders.getFilesCollection()){
                    Files.deleteIfExists(f.toPath());
                }
            Files.deleteIfExists(VirtualFolders.toPath());
            
            Files.deleteIfExists(ArtificialRoot.toPath());
            AutoBackupMaker BM = new AutoBackupMaker(LogBackupCount,HOME_DIR+"BUP","YYYY-MM-dd HH.mm.ss");
            Collection<Runnable> makeNewCopy = BM.makeNewCopy(HOME_DIR+"Log.txt");
            makeNewCopy.forEach(th ->{
                th.run();
            });
            BM.cleanUp().run();
        } catch (Exception ex) {
            ErrorReport.report(ex);
        }
        
        System.exit(0);
    }
    
}
