import java.io.File;

public class FileManager {
    public static File[] getFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        return directory.listFiles();
    }

    public static File getFileInDirectory(String fileWithPath) {
        return new File(fileWithPath);
    }
}
