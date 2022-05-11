package server;

import server.commands.Authorize;
import server.commands.interfaces.Command;
import server.commands.interfaces.DateCommand;
import core.essentials.StackInfo;
import core.essentials.Vehicle;
import core.interact.ConsoleInteractor;
import core.interact.Message;
import core.interact.UserInteractor;
import server.commands.CommandRouter;
import core.main.VehicleStackXmlParser;
import core.precommands.Precommand;
import server.database.models.UserModel;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hello world!
 */
public class App {
    public static final int port = 8001;
    private static final UserInteractor adminInteractor = new ConsoleInteractor();
    private static Stack<Vehicle> collection = new Stack<>();
    private static ZonedDateTime initDateTime;
    private static final File file = new File("collection.xml");
    private static ServerSocket serverSocket;

    private static final int MAX_CONNECTIONS = 10;
    private static final ReentrantLock lock = new ReentrantLock();

    private static final ForkJoinPool requestPool = new ForkJoinPool(MAX_CONNECTIONS);
    private static final ExecutorService processingPool = Executors.newFixedThreadPool(MAX_CONNECTIONS);
    private static final ExecutorService answerPool = Executors.newCachedThreadPool();

    private static HashMap<String, Boolean> authorizedUsers = new HashMap<>();


    public static void main(String[] args) {
        uploadInfoDB();

        if (!prepare()) {
            adminInteractor.broadcastMessage("Остановка запуска", true);
            return;
        }

        if (startServer()) {
            InetAddress inetAddress = serverSocket.getInetAddress();
            adminInteractor.broadcastMessage("Сервер запущен по адресу: " + inetAddress.getHostAddress() + ":" + port, true);
            adminInteractor.broadcastMessage("Для остановки нажмите ctrl + c", true);
        } else {
            return;
        }

        work();
    }

    private static boolean createShutdownHook() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    fileWriter.write(VehicleStackXmlParser.stackToXml(new StackInfo(collection, Vehicle.getMaxId(), initDateTime)));
                    fileWriter.flush();
                } catch (Exception e) {
                    adminInteractor.broadcastMessage(e.getMessage(), true);
                }

                adminInteractor.broadcastMessage("Остановка севера.", true);
            }));
            return true;
        } catch (Exception e) {
            adminInteractor.broadcastMessage("Не удалось настроить условие выхода.", true);
            return false;
        }
    }

    private static void uploadInfo() throws FileNotFoundException, NoSuchFieldException, IllegalAccessException {
        StackInfo stackInfo = VehicleStackXmlParser.parseFromXml(file);
        collection = Objects.requireNonNull(stackInfo).getStack();
        initDateTime = stackInfo.getCreationDate();
        Field field = Vehicle.class.getDeclaredField("maxId");
        field.setAccessible(true);
        field.setInt(null, stackInfo.getMaxId());
    }

    private static void uploadInfoDB(){

    }

    private static boolean prepare() {
        adminInteractor.broadcastMessage("Подготовка к запуску...", true);
        try {
            uploadInfo();
        } catch (FileNotFoundException | NoSuchFieldException | IllegalAccessException | NullPointerException ex) {
            if (ex instanceof NoSuchFieldException || ex instanceof IllegalAccessException || ex instanceof NullPointerException) {
                adminInteractor.broadcastMessage("Возникли проблемы при обработке файла. Данные не считаны. Создаем новый файл.", true);
            }
            initDateTime = ZonedDateTime.now();
            FileWriter fileWriter;
            try {
                fileWriter = new FileWriter(file);
                fileWriter.close();
            } catch (IOException e) {
                adminInteractor.broadcastMessage("Файл не может быть создан, недостаточно прав доступа или формат имени файла неверен.", true);
                adminInteractor.broadcastMessage("Сообщение об ошибке: " + e.getMessage(), true);
                return false;
            }
        }
        return createShutdownHook();
    }

    private static boolean startServer() {
        try {
            serverSocket = new ServerSocket(port);
            return true;
        } catch (IOException e) {
            adminInteractor.broadcastMessage(String.format("Невозможно запустить сервер (%s)%n", e.getMessage()), true);
            return false;
        }
    }

    private static void work() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                authorizedUsers.put(socket.getInetAddress().toString() + ":" + socket.getPort(), false);
                adminInteractor.broadcastMessage(String.format("Клиент (%s:%s) присоединился!", socket.getInetAddress().toString(), socket.getPort()), true);

                requestPool.execute(new RequestService(socket));

            } catch (IOException e) {
                adminInteractor.broadcastMessage("Ошибка при соединении.", true);
            }
        }
    }

    static class RequestService extends ForkJoinTask {
        private final Socket socket;

        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;

        public RequestService(Socket socket) {
            this.socket = socket;
            try {
                this.outputStream = new ObjectOutputStream(socket.getOutputStream());
                this.inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object getRawResult() {
            return null;
        }

        @Override
        protected void setRawResult(Object value) {

        }

        @Override
        protected boolean exec() {
            try {
                while (!(socket.isClosed()) && socket.isConnected()) {
                    Precommand preCommand = null;
                    try {
                        preCommand = (Precommand) inputStream.readObject();
                    } catch (ClassNotFoundException e) {
                        answerPool.submit(new AnswerService(this.outputStream, new Message("Ошибка при обработке команды.", false)));
                    } catch (IOException e) {
//                        System.out.println(e.getMessage());
                        break;
                    }
                    processingPool.submit(new ProcessingService(outputStream, preCommand, socket.getInetAddress().toString() + ":" + socket.getPort()));
                }
                adminInteractor.broadcastMessage(String.format("Клиент (%s:%s) отсоединился!", socket.getInetAddress().toString(), socket.getPort()), true);
                authorizedUsers.remove(socket.getInetAddress().toString() + ":" + socket.getPort());
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    static class ProcessingService implements Runnable {
        private final Precommand preCommand;
        private final ObjectOutputStream outputStream;
        private String currentUser = "";

        public ProcessingService(ObjectOutputStream outputStream, Precommand precommand, String currentUser) {
            this.preCommand = precommand;
            this.outputStream = outputStream;
            this.currentUser = currentUser;
        }

        @Override
        public void run() {
            Message msg;
            lock.lock();
            Command command = CommandRouter.getCommand(preCommand);
            if (command instanceof Authorize) {
                if (authorizedUsers.get(currentUser)) {
                    msg = new Message("Пользователь уже авторизован!", false);
                } else {
                    msg = command.execute(collection);
                    if (msg.isSuccessful()) {
                        authorizedUsers.put(currentUser, true);
                    }
                }

            } else if (command instanceof DateCommand && authorizedUsers.get(currentUser)) {
                msg = ((DateCommand) command).execute(collection, initDateTime);
            } else {
                if (command != null && authorizedUsers.get(currentUser)) {
                    msg = command.execute(collection);
                } else if (!authorizedUsers.get(currentUser)) {
                    msg = new Message("Только авторизованные пользователи могут выполнять команды!", true);
                } else {
                    msg = new Message("Ошибка при обработке команды.", false);
                }
            }
            lock.unlock();
            answerPool.submit(new AnswerService(this.outputStream, msg));
        }
    }

    static class AnswerService implements Runnable {
        private final ObjectOutputStream outputStream;
        private final Message msg;

        public AnswerService(ObjectOutputStream outputStream, Message msg) {
            this.outputStream = outputStream;
            this.msg = msg;
        }

        @Override
        public void run() {
            try {
                outputStream.writeObject(msg);
            } catch (IOException e) {
                adminInteractor.broadcastMessage("Клиент отсоединен.", true);
            }
        }
    }

}
