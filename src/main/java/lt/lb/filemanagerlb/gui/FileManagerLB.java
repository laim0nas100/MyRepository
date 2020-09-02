package lt.lb.filemanagerlb.gui;

import lt.lb.filemanagerlb.gui.dialog.CommandWindowController;
import lt.lb.filemanagerlb.logic.Enums;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lt.lb.commons.F;
import lt.lb.commons.Java;
import lt.lb.commons.Log;
import lt.lb.commons.containers.collections.ParametersMap;
import lt.lb.commons.io.AutoBackupMaker;
import lt.lb.commons.io.TextFileIO;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemoryPosition;
import lt.lb.commons.javafx.scenemanagement.frames.WithFrameTypeMemorySize;
import lt.lb.commons.javafx.scenemanagement.frames.WithIcon;
import lt.lb.filemanagerlb.D;
import lt.lb.filemanagerlb.logic.TaskFactory;
import lt.lb.filemanagerlb.logic.filestructure.ExtFolder;
import lt.lb.filemanagerlb.logic.filestructure.ExtPath;
import lt.lb.filemanagerlb.logic.filestructure.VirtualFolder;
import lt.lb.filemanagerlb.utility.ErrorReport;
import lt.lb.filemanagerlb.utility.FavouriteLink;
import lt.lb.filemanagerlb.utility.PathStringCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Laimonas Beniušis
 */
public class FileManagerLB {

    public static ObservableList<ExtPath> remountUpdateList = FXCollections.observableArrayList();
    public static VirtualFolder ArtificialRoot;// = new VirtualFolder(ARTIFICIAL_ROOT_DIR);
    public static VirtualFolder VirtualFolders;// = new VirtualFolder(VIRTUAL_FOLDERS_DIR);

    public static void main(String[] args) {
        D.sm = new MultiStageManager(
                new WithFrameTypeMemoryPosition(),
                new WithFrameTypeMemorySize(),
                new WithIcon(new Image(D.cLoader.getResourceAsStream("images/ico.png")))
        );

        Log.print("Manifest");

        F.unsafeRun(() -> {
            URL res = FileManagerLB.class.getResource("/stamped/version.txt");
            ArrayList<String> lines = lt.lb.commons.io.TextFileIO.readFrom(res);
            Log.print("LINES");
            Log.printLines(lines);
        });
        reInit();
        if (D.DEBUG.not().get()) {
            ViewManager.getInstance().newWebDialog(Enums.WebDialog.About);
        }

    }

    public static void remount() {
        remountUpdateList.clear();
//        remountUpdateList.add(VirtualFolders);

        ArtificialRoot.files.put(VirtualFolders.propertyName.get(), VirtualFolders);
        for (ExtPath f : ArtificialRoot.getFilesCollection()) {
            if (!Files.isDirectory(f.toPath()) && !f.isVirtual.get()) {
                ArtificialRoot.files.remove(f.propertyName.get());
            } else {
                remountUpdateList.add(f);
            }
        }

        File[] roots = File.listRoots();
        for (File root : roots) {
            mountDevice(root.getAbsolutePath());
        }
        remountUpdateList.setAll(ArtificialRoot.getFilesCollection());
    }

    public static boolean mountDevice(String name) {
        boolean result = false;
        name = name.toUpperCase();
        Log.print("Mount: " + name);
        Path path = Paths.get(name);
        if (Files.isDirectory(path)) {
            ExtFolder device = new ExtFolder(name);
            int nameCount = path.getNameCount();
            if (nameCount == 0) {
                result = true;
                String newName = path.getRoot().toString();
                device.propertyName.set(newName);
                if (!ArtificialRoot.files.containsKey(newName)) {
                    ArtificialRoot.files.put(newName, device);
                    if (!remountUpdateList.contains(device)) {
                        remountUpdateList.add(device);
                    }

                } else {
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean folderIsVirtual(ExtPath fileToCheck) {
        ExtFolder baseFolder = FileManagerLB.VirtualFolders;
        HashSet<String> set = new HashSet<>();
        for (ExtPath file : baseFolder.files.values()) {
            set.add(file.getAbsoluteDirectory());
        }
        return set.contains(fileToCheck.getAbsoluteDirectory());
    }

    public static Set<String> getRootSet() {
        return ArtificialRoot.files.keySet();
    }

    public static void doOnExit() {
        Log.print("Exit call invoked");
        ViewManager.getInstance().closeAllFramesNoExit();
        try {

//            lt.lb.commons.FileManaging.FileReader.writeToFile(USER_DIR+"Log.txt", Log.getInstance().list);
            AutoBackupMaker BM = new AutoBackupMaker(D.LogBackupCount, D.USER_DIR + "BUP", "YYYY-MM-dd HH.mm.ss");
            Log.close();
            Collection<Runnable> makeNewCopy = BM.makeNewCopy(D.logPath);
            makeNewCopy.forEach(th -> {
                th.run();
            });
            BM.cleanUp().run();
            Files.delete(Paths.get(D.logPath));

        } catch (Exception ex) {
            ErrorReport.report(ex);
        }

    }

    public static void reInit() {
        Log.print("INITIALIZE");
        ViewManager.getInstance().closeAllFramesNoExit();
        MediaPlayerController.VLCfound = false;
        MainController.actionList = new ArrayList<>();
        MainController.dragList = FXCollections.observableArrayList();
        MainController.errorLog = FXCollections.observableArrayList();
        MainController.links = FXCollections.observableArrayList();
        MainController.markedList = FXCollections.observableArrayList();
        MainController.propertyMarkedSize = Bindings.size(MainController.markedList);
        ArtificialRoot = new VirtualFolder(D.ARTIFICIAL_ROOT_DIR);
        VirtualFolders = new VirtualFolder(D.VIRTUAL_FOLDERS_DIR);
        ArtificialRoot.setIsAbsoluteRoot(true);
        CommandWindowController.executor.stopEverything();
        CommandWindowController.executor.setRunnerSize(0);
        readParameters();
        D.logPath = D.USER_DIR + Log.getZonedDateTime("HH-MM-ss") + " Log.txt";
        try {
            Path userdir = Paths.get(D.USER_DIR);
            if (!Files.isDirectory(userdir)) {
                Files.createDirectories(userdir);
            }
            Log.changeStream(Log.LogStream.FILE, D.logPath);
            Log.main().stackTrace = true;
            Logger logger = LoggerFactory.getLogger("MainLogger");
            Consumer<Supplier<String>> sl4jConsumer = (Supplier<String> str) -> {
                logger.debug(str.get());
            };
//            Log.override = sl4jCosnsumer;
//            Log.flushBuffer();
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        Log.print("Before start executor");
        CommandWindowController.executor.setRunnerSize(CommandWindowController.maxExecutablesAtOnce);
        Log.print("After start executor");
        ArtificialRoot.propertyName.set(D.ROOT_NAME);
        MainController.links.add(new FavouriteLink(D.ROOT_NAME, ArtificialRoot));
        ViewManager.getInstance().newWindow(ArtificialRoot);
        Log.print("After new window");
        //Create directories
        try {
            Files.createDirectories(Paths.get(D.USER_DIR + MediaPlayerController.PLAYLIST_DIR));
        } catch (Exception e) {
            ErrorReport.report(e);
        }

    }

    public static void readParameters() {
        ArrayDeque<String> list = new ArrayDeque<>();
        try {
            list.addAll(TextFileIO.readFromFile(D.HOME_DIR + "Parameters.txt", "//", "/*", "*/"));
        } catch (Exception e) {
            ErrorReport.report(e);
        }
        D.parameters = new ParametersMap(list, "=");
        Log.print("Parameters", D.parameters);

        D.DEBUG.set(D.parameters.defaultGet("debug", true));
        D.DEPTH = D.parameters.defaultGet("lookDepth", 2);
        D.LogBackupCount = D.parameters.defaultGet("logBackupCount", 5);
        D.ROOT_NAME = D.parameters.defaultGet("ROOT_NAME", D.ROOT_NAME);
        D.MAX_THREADS_FOR_TASK = D.parameters.defaultGet("maxThreadsForTask", TaskFactory.PROCESSOR_COUNT);
        D.USER_DIR = new PathStringCommands(D.parameters.defaultGet("userDir", D.HOME_DIR)).getPath() + File.separator;
        D.useBufferedFileStreams.setValue(D.parameters.defaultGet("bufferedFileStreams", true));
        VirtualFolder.VIRTUAL_FOLDER_PREFIX = D.parameters.defaultGet("virtualPrefix", "V");
        MediaPlayerController.VLC_SEARCH_PATH = new PathStringCommands(D.parameters.defaultGet("vlcPath", D.HOME_DIR + "lib")).getPath() + File.separator;
        PathStringCommands.number = D.parameters.defaultGet("filter.number", "#");
        PathStringCommands.fileName = D.parameters.defaultGet("filter.name", "<n>");
        PathStringCommands.nameNoExt = D.parameters.defaultGet("filter.nameNoExtension", "<nne>");
        PathStringCommands.filePath = D.parameters.defaultGet("filter.path", "<ap>");
        PathStringCommands.extension = D.parameters.defaultGet("filter.nameExtension", "<ne>");
        PathStringCommands.parent1 = D.parameters.defaultGet("filter.parent1", "<p1>");
        PathStringCommands.parent2 = D.parameters.defaultGet("filter.parent2", "<p2>");
        PathStringCommands.custom = D.parameters.defaultGet("filter.custom", "<c>");
        PathStringCommands.relativeCustom = D.parameters.defaultGet("filter.relativeCustom", "<rc>");
        CommandWindowController.commandInit = D.parameters.defaultGet("code.init", "init");
        CommandWindowController.truncateAfter = D.parameters.defaultGet("code.truncateAfter", 100000);
        CommandWindowController.maxExecutablesAtOnce = D.parameters.defaultGet("code.maxExecutables", 2);
        CommandWindowController.commandGenerate = D.parameters.defaultGet("code.commandGenerate", "generate");
        CommandWindowController.commandApply = D.parameters.defaultGet("code.commandApply", "apply");
        CommandWindowController.commandClear = D.parameters.defaultGet("code.clear", "clear");
        CommandWindowController.commandCancel = D.parameters.defaultGet("code.cancel", "cancel");
        CommandWindowController.commandList = D.parameters.defaultGet("code.list", "list");
        CommandWindowController.commandListRec = D.parameters.defaultGet("code.listRec", "listRec");
        CommandWindowController.commandSetCustom = D.parameters.defaultGet("code.setCustom", "setCustom");
        CommandWindowController.commandHelp = D.parameters.defaultGet("code.help", "help");
        CommandWindowController.commandListParams = D.parameters.defaultGet("code.listParameters", "listParams");
        CommandWindowController.maxExecutablesAtOnce = D.parameters.defaultGet("code.maxThreadsForCommand", TaskFactory.PROCESSOR_COUNT);
        CommandWindowController.commandCopyFolderStructure = D.parameters.defaultGet("code.copyFolderStructure", "copyStructure");

    }

    public static void restart() {
        try {
            Log.print("Restart request");
            FileManagerLB.doOnExit();
            System.err.println("Restart request");//Message to parent process
            Thread.sleep(10000);
            System.err.println("Failed to respond");
            System.err.println("Terminating");
        } catch (InterruptedException ex) {
        }
        System.exit(707);
    }

}
