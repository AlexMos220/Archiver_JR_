package task3110.command;



import task3110.ConsoleHelper;
import task3110.FileProperties;
import task3110.ZipFileManager;

import java.util.List;

public class ZipContentCommand extends ZipCommand {
    @Override
    public void execute() throws Exception {
        ConsoleHelper.writeMessage("Перегляд вмісту архіву.");

        ZipFileManager zipFileManager = getZipFileManager();

        ConsoleHelper.writeMessage("Вміст архіву:");

        List<FileProperties> files = zipFileManager.getFilesList();
        for (FileProperties file : files) {
            ConsoleHelper.writeMessage(file.toString());
        }

        ConsoleHelper.writeMessage("Вміст архіву прочитано.");
    }
}
