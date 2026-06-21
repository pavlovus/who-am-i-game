# Вижимка лекцій — інструменти та практики для проєкту

> Тільки те, що безпосередньо знадобиться при розробці клієнт-серверного застосунку на Java.

---

## 1. Мережа (Лекція 3)

### Java Network API — TCP

```java
// Сервер: приймати нові з'єднання
ServerSocket serverSocket = new ServerSocket(3333);
while (true) {
    Socket socket = serverSocket.accept(); // блокується до нового клієнта
}
```

```java
// Клієнт/сервер: читання та запис через сокет
InputStream  in  = socket.getInputStream();
OutputStream out = socket.getOutputStream();
// Завжди закривайте сокет після завершення роботи
socket.close();
```

**Кілька клієнтів одночасно:**
- Головний потік приймає з'єднання через `ServerSocket`
- Кожен клієнт обробляється у власному окремому потоці

### Java Network API — UDP

```java
// Сервер
DatagramSocket serverSocket = new DatagramSocket(4445);

// Клієнт (без прив'язки до порту)
DatagramSocket clientSocket = new DatagramSocket();

// Надіслати пакет
byte[] buf = "Hello".getBytes();
InetAddress address = InetAddress.getByName("localhost");
DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
clientSocket.send(packet);

// Отримати пакет
byte[] buf = new byte[256];
DatagramPacket packet = new DatagramPacket(buf, buf.length);
serverSocket.receive(packet);
```

> **UDP + NAT:** використовуйте heartbeat — клієнт регулярно надсилає порожні пакети, щоб роутер не закривав запис у таблиці NAT.

---

## 2. Багатопоточність (Лекція 2)

### Створення потоків

```java
// Варіант 1 — через Runnable (рекомендований)
Runnable r = () -> System.out.println("task");
Thread t = new Thread(r);
t.start();

// Варіант 2 — через успадкування
class MyThread extends Thread {
    @Override
    public void run() { /* логіка */ }
}
```

### Thread API

```java
t.start();           // запустити потік
t.join();            // чекати завершення потоку t
Thread.sleep(1000);  // призупинити поточний потік на 1с
t.interrupt();       // перервати потік
```

### Race Condition — рішення

**Рішення 1 — Atomic-класи** (для простих лічильників):
```java
import java.util.concurrent.atomic.AtomicInteger;
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
```

Також: `AtomicLong`, `AtomicIntegerArray`, `AtomicBoolean`

**Рішення 2 — `synchronized`** (для складніших блоків):
```java
Object lock = new Object();
synchronized (lock) {
    x++; // лише один потік одночасно
}
```

### Взаємодія між потоками

```java
// Всередині synchronized-блоку:
lock.wait();       // призупинити і відпустити lock
lock.notify();     // розбудити один потік
lock.notifyAll();  // розбудити всі потоки
```

> Умову перед `wait()` перевіряйте через `while`, а не `if`.

### ExecutorService (пул потоків)

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

ExecutorService pool = Executors.newFixedThreadPool(4);
pool.submit(() -> { /* задача */ });               // Runnable
Future<String> future = pool.submit(() -> "result"); // Callable з результатом
```

### Потокобезпечні колекції

```java
import java.util.concurrent.*;

ArrayBlockingQueue<T>   queue = new ArrayBlockingQueue<>(100);
ConcurrentHashMap<K,V>  map   = new ConcurrentHashMap<>();
CopyOnWriteArrayList<T> list  = new CopyOnWriteArrayList<>();
```

> ⚠️ Ніколи не використовуйте `ArrayList`, `HashMap` тощо спільно між потоками без синхронізації.

---

## 3. JDBC — робота з БД (Лекція 4)

### Maven-залежності

```xml
<!-- SQLite -->
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- PostgreSQL -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### З'єднання з БД

```java
// SQLite (файл)
Connection con = DriverManager.getConnection("jdbc:sqlite:my_database.db");

// MySQL
Connection con = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/dbname", "username", "password"
);
```

### Statement — прості запити

```java
try (Connection con = DriverManager.getConnection(url);
     Statement stmt = con.createStatement()) {

    // SELECT
    ResultSet rs = stmt.executeQuery("SELECT id, name FROM users");
    while (rs.next()) {
        int    id   = rs.getInt("id");
        String name = rs.getString("name");
    }

    // INSERT / UPDATE / DELETE — повертає кількість змінених рядків
    int rows = stmt.executeUpdate("DELETE FROM users WHERE id = 5");
}
```

### PreparedStatement — запити з параметрами ✅ (рекомендований)

```java
try (PreparedStatement pstmt = con.prepareStatement(
        "INSERT INTO users (name, password_hash) VALUES (?, ?)")) {

    pstmt.setString(1, "john");
    pstmt.setString(2, hashedPassword);
    pstmt.executeUpdate();
}
```

```java
// Повторне виконання з новими параметрами
PreparedStatement pstmt = con.prepareStatement(
    "UPDATE sessions SET token = ? WHERE user_id = ?"
);
for (User u : users) {
    pstmt.setString(1, u.getToken());
    pstmt.setLong(2, u.getId());
    pstmt.executeUpdate();
}
```

> ✅ `PreparedStatement` захищає від SQL-ін'єкцій та ефективніший при повторних запитах.

> ⚠️ Завжди використовуйте **try-with-resources** для `Connection`, `Statement`, `ResultSet`.

### Відображення типів JDBC ↔ Java

| Java | JDBC / SQL |
|---|---|
| `String` | `VARCHAR`, `CHAR` |
| `int` | `INTEGER` |
| `long` | `BIGINT` |
| `boolean` | `BIT` |
| `double` | `DOUBLE` |
| `byte[]` | `VARBINARY` |
| `java.sql.Date` | `DATE` |
| `java.sql.Timestamp` | `TIMESTAMP` |

### Транзакції

```java
con.setAutoCommit(false); // вимкнути auto-commit
try {
    // кілька операцій
    stmt.executeUpdate("...");
    stmt.executeUpdate("...");
    con.commit();
} catch (SQLException e) {
    con.rollback();
}
```

### Тестування з TestContainers

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
class BaseMySqlTest {
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>();

    @BeforeAll
    static void beforeAll() { MYSQL.start(); }

    @AfterAll
    static void afterAll() { MYSQL.stop(); }
}
```

Документація: https://java.testcontainers.org/modules/databases/

---

## 4. HTTP-сервер (Лекція 5)

### Запуск сервера

```java
import com.sun.net.httpserver.HttpServer;

HttpServer server = HttpServer.create(new InetSocketAddress(8181), 0);
server.start();
```

### Реєстрація обробника (Handler)

```java
server.createContext("/users/", exchange -> {
    String method = exchange.getRequestMethod(); // "GET", "POST", ...
    URI    uri    = exchange.getRequestURI();     // /users/10?foo=bar
    // ...
});
```

> ⚠️ Шлях у `createContext` є **префіксом** — `/users/` спіймає всі запити, що починаються з цього рядка.

### HttpExchange — читання запиту та відправка відповіді

```java
exchange.getRequestMethod()                      // HTTP-метод
exchange.getRequestURI()                         // повний URI з query params
exchange.getRequestHeaders()                     // заголовки запиту
exchange.getRequestBody()                        // InputStream тіла

exchange.getResponseHeaders().set("Content-Type", "application/json");
exchange.sendResponseHeaders(200, responseBytes.length);

try (OutputStream os = exchange.getResponseBody()) {
    os.write(responseBytes);
}
```

> ⚠️ Завжди використовуйте **try-with-resources** для `getResponseBody()`.

### Авторизація

**Basic Auth** (логін:пароль у Base64):
```
Authorization: Basic base64(username:password)
```

**Bearer Auth** (токен/JWT):
```
Authorization: Bearer <token>
```

**Додавання Authenticator до контексту:**
```java
HttpContext ctx = server.createContext("/api/", handler);
ctx.setAuthenticator(new Authenticator() {
    @Override
    public Result authenticate(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (isValid(header)) {
            return new Success(new HttpPrincipal("user", "realm"));
        }
        return new Failure(401);
    }
});
```

Результати: `Success`, `Failure(statusCode)`, `Retry`

### Тестування HTTP — RestAssured

```xml
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

---

## 5. Швидка довідка по пакетам

| Задача | Клас / пакет |
|---|---|
| TCP-сервер | `java.net.ServerSocket` |
| TCP-клієнт | `java.net.Socket` |
| UDP | `java.net.DatagramSocket`, `DatagramPacket` |
| IP-адреса | `java.net.InetAddress` |
| Потоки | `java.lang.Thread`, `Runnable` |
| Пул потоків | `java.util.concurrent.ExecutorService` |
| Атомарні операції | `java.util.concurrent.atomic.*` |
| Потокобезпечні колекції | `java.util.concurrent.*` |
| JDBC з'єднання | `java.sql.DriverManager`, `Connection` |
| JDBC запити | `Statement`, `PreparedStatement`, `ResultSet` |
| HTTP-сервер | `com.sun.net.httpserver.HttpServer` |
| HTTP-обробник | `HttpHandler`, `HttpExchange` |
| HTTP-авторизація | `com.sun.net.httpserver.Authenticator` |
