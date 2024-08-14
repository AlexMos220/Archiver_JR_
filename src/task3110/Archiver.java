package task3110;


import task3110.exception.WrongZipFileException;

import java.io.IOException;

public class Archiver {
    public static void main(String[] args) throws IOException {

        Operation operation = null;
        do {
            try {
                operation = askOperation();
                CommandExecutor.execute(operation);
            } catch (WrongZipFileException e) {
                ConsoleHelper.writeMessage("Ви не вибрали файл архіву або вибрали неправильний файл.");
            } catch (Exception e) {
                ConsoleHelper.writeMessage("Відбулася помилка. Перевірте введені дані.");
            }

        } while (operation != Operation.EXIT);
    }


    public static Operation askOperation() throws IOException {
        ConsoleHelper.writeMessage("");
        ConsoleHelper.writeMessage("Виберіть операцію:");
        ConsoleHelper.writeMessage(String.format("\t %d - упакувати файли в архів", Operation.CREATE.ordinal()));
        ConsoleHelper.writeMessage(String.format("\t %d - додати файл до архіву", Operation.ADD.ordinal()));
        ConsoleHelper.writeMessage(String.format("\t %d - видалити файл з архіву", Operation.REMOVE.ordinal()));
        ConsoleHelper.writeMessage(String.format("\t %d - розпакувати архів", Operation.EXTRACT.ordinal()));
        ConsoleHelper.writeMessage(String.format("\t %d - переглянути вміст архіву", Operation.CONTENT.ordinal()));
        ConsoleHelper.writeMessage(String.format("\t %d - вихід", Operation.EXIT.ordinal()));

        return Operation.values()[ConsoleHelper.readInt()];
    }
}