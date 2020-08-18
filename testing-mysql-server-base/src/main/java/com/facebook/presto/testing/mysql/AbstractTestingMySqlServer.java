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
import com.google.common.collect.ImmutableSet;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class AbstractTestingMySqlServer
        implements Closeable
{
    private static final Logger log = Logger.get(AbstractTestingMySqlServer.class);

    // for ppc64le, mariadb 10.2.x is used as an alternative for mysql 5.7
    private static final boolean isMariadb = System.getProperty("os.arch").equals("ppc64le");

    private final String user;
    private final String password;
    private final Set<String> databases;
    private final int port;
    private final String version;
    private final EmbeddedMySql server;

    public AbstractTestingMySqlServer(EmbeddedMySql server, String user, String password, Iterable<String> databases)
            throws Exception
    {
        this.server = requireNonNull(server, "server is null");
        this.user = requireNonNull(user, "user is null");
        this.password = requireNonNull(password, "password is null");
        this.databases = ImmutableSet.copyOf(requireNonNull(databases, "databases is null"));
        this.port = server.getPort();

        try (Connection connection = server.getMySqlDatabase()) {
            version = connection.getMetaData().getDatabaseProductVersion();
            try (Statement statement = connection.createStatement()) {
                execute(statement, format("CREATE USER '%s'@'localhost' IDENTIFIED BY '%s'", user, password));
                execute(statement, format("GRANT ALL ON *.* to '%s'@'localhost' WITH GRANT OPTION", user));
                for (String database : databases) {
                    execute(statement, format("CREATE DATABASE %s", database));
                }
            }
        }
        catch (SQLException e) {
            close();
            throw e;
        }

        log.info("MySQL server ready: %s", getJdbcUrl());
    }

    private static void execute(Statement statement, String sql)
            throws SQLException
    {
        log.debug("Executing: %s", sql);
        statement.execute(sql);
    }

    @Override
    public void close()
            throws IOException
    {
        server.close();
    }

    public String getMySqlVersion()
    {
        return version;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public Set<String> getDatabases()
    {
        return databases;
    }

    public int getPort()
    {
        return port;
    }

    public String getJdbcUrl()
    {
        return getJdbcUrl("");
    }

    public abstract String getJdbcUrl(String database);
}
