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

import static java.util.Arrays.asList;

public class TestTestingMySqlServer
        extends AbstractTestTestingMySqlServer
{
    // for ppc64le, mariadb 10.2.x is used as an alternative for mysql 5.7
    private static final boolean isMariadb = System.getProperty("os.arch").equals("ppc64le");

    @Override
    public String getMySqlVersion()
    {
        return (isMariadb ? "5.5.5-10.2.32-MariaDB" : "5.7.22");
    }

    @Override
    public AbstractTestingMySqlServer createMySqlServer(String user, String password, String... databases)
            throws Exception
    {
        return new TestingMySqlServer(user, password, asList(databases), MySqlOptions.builder().build());
    }
}
