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

import java.io.IOException;
import java.util.List;

final class EmbeddedMySql8
        extends AbstractEmbeddedMySql
{
    public EmbeddedMySql8(MySqlOptions mySqlOptions)
            throws IOException
    {
        super(mySqlOptions);
    }

    @Override
    public List<String> getInitializationArguments()
    {
        return ImmutableList.of(
                "--no-defaults",
                "--initialize-insecure",
                "--innodb-flush-method=nosync",
                "--datadir", getDataDirectory());
    }

    @Override
    public List<String> getStartArguments()
    {
        return ImmutableList.of(
                "--no-defaults",
                "--skip-mysqlx",
                "--default-time-zone=+00:00",
                "--innodb-flush-method=nosync",
                "--innodb-flush-log-at-trx-commit=0",
                "--innodb-doublewrite=0",
                "--bind-address=localhost",
                "--lc_messages_dir", getShareDirectory(),
                "--socket", getSocketDirectory(),
                "--port", String.valueOf(getPort()),
                "--datadir", getDataDirectory());
    }
}
