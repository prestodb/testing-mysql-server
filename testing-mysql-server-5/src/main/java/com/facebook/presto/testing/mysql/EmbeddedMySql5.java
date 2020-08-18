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

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Arrays;
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
        List<String> list = Arrays.asList(
                "--no-defaults",
                "--skip-sync-frm",
                "--innodb-flush-method=nosync",
                "--datadir=" + getDataDirectory());

        if (isMariadb) {
            return ImmutableList.<String>builder().addAll(list).add("--basedir=" + getBaseDirectory()).build();
        }
        else {
            return ImmutableList.<String>builder().addAll(list).add("--initialize-insecure").build();
        }
    }

    @Override
    public List<String> getStartArguments() throws VerifyException
    {
        List<String> list = Arrays.asList(
                "--no-defaults",
                "--default-time-zone=+00:00",
                "--skip-sync-frm",
                "--innodb-flush-method=nosync",
                "--innodb-flush-log-at-trx-commit=0",
                "--innodb-doublewrite=0",
                "--bind-address=localhost",
                "--port=" + String.valueOf(getPort()),
                "--datadir=" + getDataDirectory(),
                "--socket=" + getSocketDirectory());

        if (isMariadb) {
            return ImmutableList.<String>builder().addAll(list).add(
                "--basedir=" + getBaseDirectory(),
                "--plugin-dir=" + getMariadbPluginDirectory(),
                "--log-error=" + getDataDirectory() + "mariadb.log",
                "--pid-file=" + getDataDirectory() + "mariadb.pid").build();
        }
        else {
            return ImmutableList.<String>builder().addAll(list).add(
                "--skip-ssl",
                "--disable-partition-engine-check",
                "--explicit_defaults_for_timestamp",
                "--lc_messages_dir=" + getShareDirectory()).build();
        }
    }
}
