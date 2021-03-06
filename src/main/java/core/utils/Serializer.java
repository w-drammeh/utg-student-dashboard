package core.utils;

import static core.utils.Globals.joinPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;

import core.News;
import core.Portal;
import core.alert.Notification;
import core.driver.MDriver;
import core.module.ModuleHandler;
import core.module.SemesterActivity;
import core.setting.Settings;
import core.task.TaskActivity;
import core.user.Student;
import utg.Dashboard;

public class Serializer {

    /**
     * Serializes the given object to the given path.
     * Classes that perform serialization eventually invoke this
     * in order to do the ultimate writing.
     * This method is self-silent.
     */
    public static void toDisk(Object obj, String path){
        try {
            final File file = new File(path);
            final File parentFile = file.getParentFile();
            if (parentFile.exists() || parentFile.mkdirs()) {
                final FileOutputStream fileOutputStream = new FileOutputStream(path);
                final ObjectOutputStream objOutputStream = new ObjectOutputStream(fileOutputStream);
                objOutputStream.writeObject(obj);
                objOutputStream.close();
            } else {
                App.silenceException("Failed to mount parent directories to serialize file '" + path + "'.");
            }
        } catch (IOException e) {
            App.silenceException(e);
        }
    }

    /**
     * Reads the object that was serialized to the given path.
     * Classes that perform de-serialization eventually
     * invoke this to do the ultimate reading.
     * This method is self-silent.
     * Therefore, callers must check nullity of the returned object
     */
    public static Object fromDisk(String path) {
        Object serObject = null;
        try {
            final FileInputStream fileInputStream = new FileInputStream(path);
            final ObjectInputStream objInputStream = new ObjectInputStream(fileInputStream);
            serObject = objInputStream.readObject();
            objInputStream.close();
            fileInputStream.close();
        } catch (IOException | ClassNotFoundException e) {
            App.silenceException(e);
        }
        return serObject;
    }

    public static String inPath(String... paths){
        return String.join(File.separator, Dashboard.getPath(), joinPaths(paths));
    }

    public static void mountUserData(){
        Dashboard.storeConfigs();
        Settings.serialize();
        if (!Student.isGuest()) {
            Portal.serialize();
            SemesterActivity.serialize();
            ModuleHandler.serialize();
        }
        Student.serialize();
        TaskActivity.serializeAll();
        Notification.serialize();
        News.serialize();
        MDriver.stopAll();
    }

    public static boolean unMountUserData(){
        try {
            FileUtils.deleteDirectory(new File(Dashboard.getPath()));
            return true;
        } catch (IOException e) {
            App.silenceException(e);
            final File configFile = new File(inPath("configs.ser"));
            return configFile.delete();
        }
    }

}
