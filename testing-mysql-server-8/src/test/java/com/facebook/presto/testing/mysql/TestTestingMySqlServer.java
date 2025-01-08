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

import java.util.Arrays;

public class TestTestingMySqlServer
        extends AbstractTestTestingMySqlServer
{
    @Override
    public String getMySqlVersion()
    {
        return "8.4.3";
    }

    @Override
    public AbstractTestingMySqlServer createMySqlServer(String user, String password, String... databases)
            throws Exception
    {
        return new TestingMySqlServer(user, password, Arrays.asList(databases), MySqlOptions.builder().build());
    }
}
