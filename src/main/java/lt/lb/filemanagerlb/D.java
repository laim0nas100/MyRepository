package lt.lb.filemanagerlb;

import lt.lb.filemanagerlb.dirinfo.HomeDir;
import java.io.File;
import javafx.beans.property.SimpleBooleanProperty;
import lt.lb.commons.containers.collections.ParametersMap;
import lt.lb.commons.io.directoryaccess.Dir;
import lt.lb.commons.javafx.scenemanagement.MultiStageManager;
import lt.lb.filemanagerlb.utility.PathStringCommands;

/**
 *
 * @author Lemmin
 */
public class D {

    public static SessionInfo sessionInfo = new SessionInfo();
    public static final HomeDir HOME_DIR = Dir.establishDirectory(System.getProperty("user.home") + File.separator + ".FileManagerLB", HomeDir.class);
    public static final String VIRTUAL_FOLDERS_DIR = HOME_DIR.getAbsolutePathWithSeparator() + "VIRTUAL_FOLDERS" + File.separator;
    public static final String ARTIFICIAL_ROOT_DIR = HOME_DIR.getAbsolutePathWithSeparator() + "ARTIFICIAL_ROOT";
    public static String USER_DIR = HOME_DIR.getAbsolutePathWithSeparator();
    public static String ROOT_NAME = "ROOT";
    public static int MAX_THREADS_FOR_TASK = 10;
    public static int DEPTH = 1;
    public static SimpleBooleanProperty DEBUG = new SimpleBooleanProperty(false);
    public static int LogBackupCount = 1;
    public static SimpleBooleanProperty useBufferedFileStreams = new SimpleBooleanProperty(true);
    public static ParametersMap parameters;
    public static PathStringCommands customPath = new PathStringCommands(HOME_DIR.absolutePath);

    public static MultiStageManager sm;

    public static ClassLoader cLoader = D.class.getClassLoader();
}
