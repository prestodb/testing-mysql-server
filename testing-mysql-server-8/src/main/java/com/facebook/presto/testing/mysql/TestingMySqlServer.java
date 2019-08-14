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

import com.google.common.collect.ImmutableList;

import static java.lang.String.format;

public final class TestingMySqlServer
        extends AbstractTestingMySqlServer
{
    public TestingMySqlServer(String user, String password, String... databases)
            throws Exception
    {
        this(user, password, ImmutableList.copyOf(databases));
    }

    public TestingMySqlServer(String user, String password, Iterable<String> databases)
            throws Exception
    {
        super(new EmbeddedMySql8(), user, password, databases);
    }

    @Override
    public String getJdbcUrl(String database)
    {
        return format("jdbc:mysql://localhost:%s/%s?user=%s&password=%s&useSSL=false&allowPublicKeyRetrieval=true", getPort(), database, getUser(), getPassword());
    }
}
