package task3110;


import task3110.exception.PathIsNotFoundException;
import task3110.exception.WrongZipFileException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileManager {
    // Повний шлях zip файлу
    private final Path zipFile;

    public ZipFileManager(Path zipFile) {
        this.zipFile = zipFile;
    }

    public void createZip(Path source) throws Exception {
        // Перевіряємо, чи існує директорія, де створюватиметься архів
        // За потреби створюємо її
        Path zipDirectory = zipFile.getParent();
        if (Files.notExists(zipDirectory))
            Files.createDirectories(zipDirectory);

        // Створюємо zip потік
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile))) {

            if (Files.isDirectory(source)) {
                // Якщо архівуємо директорію, потрібно отримати список файлів в ній
                FileManager fileManager = new FileManager(source);
                List<Path> fileNames = fileManager.getFileList();

                // Додаємо кожен файл до архіву
                for (Path fileName : fileNames)
                    addNewZipEntry(zipOutputStream, source, fileName);

            } else if (Files.isRegularFile(source)) {

                // Якщо архівуємо окремий файл, потрібно отримати його директорію та ім'я
                addNewZipEntry(zipOutputStream, source.getParent(), source.getFileName());
            } else {

                // Якщо переданий source не директорія і файл, кидаємо виняток
                throw new PathIsNotFoundException();
            }
        }
    }

    public void extractAll(Path outputFolder) throws Exception {
        // Перевіряємо, чи існує zip файл
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            // Створюємо директорію виведення, якщо вона не існує
            if (Files.notExists(outputFolder))
                Files.createDirectories(outputFolder);

            // Проходимося по вмісту zip потоку (файлу)
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                String fileName = zipEntry.getName();
                Path fileFullName = outputFolder.resolve(fileName);

                // Створюємо необхідні директорії
                Path parent = fileFullName.getParent();
                if (Files.notExists(parent))
                    Files.createDirectories(parent);

                try (OutputStream outputStream = Files.newOutputStream(fileFullName)) {
                    copyData(zipInputStream, outputStream);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public void removeFile(Path path) throws Exception {
        removeFiles(Collections.singletonList(path));
    }

    public void removeFiles(List<Path> pathList) throws Exception {
        // Перевіряємо, чи існує zip файл
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        // Створюємо тимчасовий файл
        Path tempZipFile = Files.createTempFile(null, null);

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {

                    Path archivedFile = Paths.get(zipEntry.getName());

                    if (!pathList.contains(archivedFile)) {
                        String fileName = zipEntry.getName();
                        zipOutputStream.putNextEntry(new ZipEntry(fileName));

                        copyData(zipInputStream, zipOutputStream);

                        zipOutputStream.closeEntry();
                        zipInputStream.closeEntry();
                    } else {
                        ConsoleHelper.writeMessage(String.format("Файл '%s' видалено з архіву.", archivedFile.toString()));
                    }
                    zipEntry = zipInputStream.getNextEntry();
                }
            }
        }

        // Переміщуємо тимчасовий файл на місце оригінального
        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public void addFile(Path absolutePath) throws Exception {
        addFiles(Collections.singletonList(absolutePath));
    }

    public void addFiles(List<Path> absolutePathList) throws Exception {
        // Перевіряємо, чи існує zip файл
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        // Створюємо тимчасовий файл
        Path tempZipFile = Files.createTempFile(null, null);
        List<Path> archiveFiles = new ArrayList<>();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {

                ZipEntry zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    String fileName = zipEntry.getName();
                    archiveFiles.add(Paths.get(fileName));

                    zipOutputStream.putNextEntry(new ZipEntry(fileName));
                    copyData(zipInputStream, zipOutputStream);

                    zipInputStream.closeEntry();
                    zipOutputStream.closeEntry();

                    zipEntry = zipInputStream.getNextEntry();
                }
            }

            // Архівуємо нові файли
            for (Path file : absolutePathList) {
                if (Files.isRegularFile(file)) {
                    if (archiveFiles.contains(file.getFileName()))
                        ConsoleHelper.writeMessage(String.format("Файл '%s' вже існує у архіві.", file.toString()));
                    else {
                        addNewZipEntry(zipOutputStream, file.getParent(), file.getFileName());
                        ConsoleHelper.writeMessage(String.format("Файл '%s' доданий до архіву.", file.toString()));
                    }
                } else
                    throw new PathIsNotFoundException();
            }
        }

        // Переміщуємо тимчасовий файл на місце оригінального
        Files.move(tempZipFile, zipFile, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<FileProperties> getFilesList() throws Exception {
        // Перевіряємо, чи існує zip файл
        if (!Files.isRegularFile(zipFile)) {
            throw new WrongZipFileException();
        }

        List<FileProperties> files = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                // Поля "розмір" та "стислий розмір" не відомі, поки елемент не буде прочитаний
                // Давайте прочитаємо його у якийсь вихідний потік
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                copyData(zipInputStream, baos);

                FileProperties file = new FileProperties(zipEntry.getName(), zipEntry.getSize(), zipEntry.getCompressedSize(), zipEntry.getMethod());
                files.add(file);
                zipEntry = zipInputStream.getNextEntry();
            }
        }

        return files;
    }

    private void addNewZipEntry(ZipOutputStream zipOutputStream, Path filePath, Path fileName) throws Exception {
        Path fullPath = filePath.resolve(fileName);
        try (InputStream inputStream = Files.newInputStream(fullPath)) {
            ZipEntry entry = new ZipEntry(fileName.toString());

            zipOutputStream.putNextEntry(entry);

            copyData(inputStream, zipOutputStream);

            zipOutputStream.closeEntry();
        }
    }

    private void copyData(InputStream in, OutputStream out) throws Exception {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }
}
