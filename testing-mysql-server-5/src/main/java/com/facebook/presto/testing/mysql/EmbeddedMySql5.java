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

final class EmbeddedMySql5
        extends AbstractEmbeddedMySql
{
    public EmbeddedMySql5(MySqlOptions mySqlOptions)
            throws IOException
    {
        super(mySqlOptions);
    }

    @Override
    public List<String> getInitializationArguments()
    {
        if (isMariadb) {
            return ImmutableList.of(
                "--no-defaults",
                "--basedir=" + getBaseDirectory(),
                "--datadir=" + getDataDirectory(),
                "--skip-sync-frm",
                "--innodb-flush-method=nosync");
        }
        else {
            return ImmutableList.of(
                    "--no-defaults",
                    "--initialize-insecure",
                    "--skip-sync-frm",
                    "--innodb-flush-method=nosync",
                    "--datadir", getDataDirectory());
        }
    }

    @Override
    public List<String> getStartArguments()
    {
        if (isMariadb) {
            return ImmutableList.of(
                    "--no-defaults",
                    "--default-time-zone=+00:00",
                    "--skip-sync-frm",
                    "--innodb-flush-method=nosync",
                    "--innodb-flush-log-at-trx-commit=0",
                    "--innodb-doublewrite=0",
                    "--bind-address=localhost",
                    "--basedir=" + getBaseDirectory(),
                    "--plugin-dir=" + getPluginDirectory(),
                    "--log-error=" + getDataDirectory() + "mariadb.log",
                    "--pid-file=" + getDataDirectory() + "mariadb.pid",
                    "--socket=" + getDataDirectory() + "mysql.sock",
                    "--port=" + String.valueOf(getPort()),
                    "--datadir=" + getDataDirectory());
        }
        else {
            return ImmutableList.of(
                    "--no-defaults",
                    "--skip-ssl",
                    "--default-time-zone=+00:00",
                    "--disable-partition-engine-check",
                    "--explicit_defaults_for_timestamp",
                    "--skip-sync-frm",
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
}
