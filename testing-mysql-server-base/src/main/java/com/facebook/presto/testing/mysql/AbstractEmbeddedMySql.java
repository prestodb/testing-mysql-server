/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.testing.mysql;

import com.facebook.airlift.log.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.units.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.facebook.airlift.concurrent.Threads.daemonThreadsNamed;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.StandardSystemProperty.OS_ARCH;
import static com.google.common.base.StandardSystemProperty.OS_NAME;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static java.io.File.createTempFile;
import static java.lang.String.format;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class AbstractEmbeddedMySql
        implements EmbeddedMySql
{
    private static final Logger log = Logger.get(AbstractEmbeddedMySql.class);

    private static final String JDBC_FORMAT = "jdbc:mysql://localhost:%s/%s?user=%s&useSSL=false&serverTimezone=" + TimeZone.getDefault().getID();

    private final ExecutorService executor = newCachedThreadPool(daemonThreadsNamed("testing-mysql-server-%s"));
    private final Path serverDirectory;
    private final int port = randomPort();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Process mysqld;

    private final Duration startupWait;
    private final Duration shutdownWait;
    private final Duration commandTimeout;

    // for ppc64le, mariadb 10.2.x is used as an alternative for mysql 5.7
    protected final boolean isMariadb = System.getProperty("os.arch").equals("ppc64le");

    public AbstractEmbeddedMySql(MySqlOptions mySqlOptions)
            throws IOException
    {
        this.startupWait = requireNonNull(mySqlOptions.getStartupWait(), "startupWait is null");
        this.shutdownWait = requireNonNull(mySqlOptions.getShutdownWait(), "shutdownWait is null");
        this.commandTimeout = requireNonNull(mySqlOptions.getCommandTimeout(), "commandTimeout is null");

        serverDirectory = createTempDirectory("testing-mysql-server");

        log.info("Starting MySQL server in %s", serverDirectory);

        try {
            unpackMySql(serverDirectory);
            initialize();
            mysqld = startMysqld();
        }
        catch (Exception e) {
            close();
            throw e;
        }
    }

    public abstract List<String> getInitializationArguments();

    public abstract List<String> getStartArguments();

    public String getJdbcUrl(String userName, String dbName)
    {
        return format(JDBC_FORMAT, port, dbName, userName);
    }

    public int getPort()
    {
        return port;
    }

    public Connection getMySqlDatabase()
            throws SQLException
    {
        return DriverManager.getConnection(getJdbcUrl("root", "mysql"));
    }

    protected String getMysqlInstallDb()
    {
        return (isMariadb ? serverDirectory.resolve("bin").resolve("mysql_install_db").toString() : "");
    }

    protected String getBaseDirectory()
    {
        return serverDirectory.toString();
    }

    protected String getMysqld()
    {
        return serverDirectory.resolve("bin").resolve("mysqld").toString();
    }

    protected String getDataDirectory()
    {
        return serverDirectory.resolve("data").toString();
    }

    protected String getShareDirectory()
    {
        return serverDirectory.resolve("share").toString();
    }

    protected String getSocketDirectory()
    {
        return (isMariadb ? "" : serverDirectory.resolve("mysql.sock").toString());
    }

    protected String getPluginDirectory()
    {
        return (isMariadb ? serverDirectory.resolve("lib64").resolve("mysql").resolve("plugin").toString() : "");
    }

    @Override
    public void close()
    {
        if (closed.getAndSet(true)) {
            return;
        }

        if (mysqld != null) {
            log.info("Shutting down mysqld. Waiting up to %s for shutdown to finish.", startupWait);

            mysqld.destroyForcibly();

            try {
                mysqld.waitFor(shutdownWait.toMillis(), MILLISECONDS);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (mysqld.isAlive()) {
                log.error("mysqld is still running in %s", serverDirectory);
            }
        }

        try {
            deleteRecursively(serverDirectory, ALLOW_INSECURE);
        }
        catch (IOException e) {
            log.warn(e, "Failed to delete %s", serverDirectory);
        }

        executor.shutdownNow();
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("serverDirectory", serverDirectory)
                .add("port", port)
                .toString();
    }

    private static int randomPort()
            throws IOException
    {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void initialize()
    {
        if (isMariadb) {
            system(ImmutableList.<String>builder()
                    .add(getMysqlInstallDb())
                    .addAll(getInitializationArguments())
                    .build());
        }
        else {
            system(ImmutableList.<String>builder()
                    .add(getMysqld())
                    .addAll(getInitializationArguments())
                    .build());
        }
    }

    private Process startMysqld()
            throws IOException
    {
        Process process = new ProcessBuilder(ImmutableList.<String>builder().add(getMysqld()).addAll(getStartArguments()).build())
                .redirectErrorStream(true)
                .start();

        log.info("mysqld started on port %s. Waiting up to %s for startup to finish.", port, startupWait);

        startOutputProcessor(process.getInputStream());

        waitForServerStartup(process);

        return process;
    }

    private void waitForServerStartup(Process process)
            throws IOException
    {
        Throwable lastCause = null;
        long start = System.nanoTime();
        while (Duration.nanosSince(start).compareTo(startupWait) <= 0) {
            try {
                checkReady();
                log.info("mysqld startup finished");
                return;
            }
            catch (SQLException e) {
                lastCause = e;
            }

            try {
                // check if process has exited
                int value = process.exitValue();
                throw new IOException(format("mysqld exited with value %s, check stdout for more detail", value));
            }
            catch (IllegalThreadStateException ignored) {
                // process is still running, loop and try again
            }

            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        throw new IOException("mysqld failed to start after " + startupWait, lastCause);
    }

    private void checkReady()
            throws SQLException
    {
        try (Connection connection = getMySqlDatabase();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 42")) {
            checkSql(resultSet.next(), "no rows in result set");
            checkSql(resultSet.getInt(1) == 42, "wrong result");
            checkSql(!resultSet.next(), "multiple rows in result set");
        }
    }

    private static void checkSql(boolean expression, String message)
            throws SQLException
    {
        if (!expression) {
            throw new SQLException(message);
        }
    }

    private void startOutputProcessor(InputStream in)
    {
        executor.execute(() -> {
            try {
                ByteStreams.copy(in, System.out);
            }
            catch (IOException ignored) {
            }
        });
    }

    private void system(List<String> command)
    {
        try {
            new Command(command.toArray(new String[0]))
                    .setTimeLimit(commandTimeout)
                    .execute(executor);
        }
        catch (CommandFailedException e) {
            throw new RuntimeException(e);
        }
    }

    private void unpackMySql(Path target)
            throws IOException
    {
        String archiveName = format("/mysql-%s.tar.gz", getPlatform());
        URL url = AbstractEmbeddedMySql.class.getResource(archiveName);
        if (url == null) {
            throw new RuntimeException("archive not found: " + archiveName);
        }

        File archive = createTempFile("mysql-", null);
        try {
            try (InputStream in = url.openStream()) {
                copy(in, archive.toPath(), REPLACE_EXISTING);
            }
            system(ImmutableList.of("tar", "-xf", archive.getPath(), "-C", target.toString()));
        }
        finally {
            if (!archive.delete()) {
                log.warn("Failed to delete file %s", archive);
            }
        }
    }

    private static String getPlatform()
    {
        return (OS_NAME.value() + "-" + OS_ARCH.value()).replace(' ', '_');
    }
}
