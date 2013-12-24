/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.db.gora;

import java.util.ArrayList;
import java.util.List;

/**
  * Defines the SQLite table info
  * See {@link org.db.gora.DatabaseHelper}
  *
  * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

class DbTableInfo {
    public DbTableInfo(String tableName, String pkName) {
        this.tableName = tableName;
        this.pkName = pkName;
    }

    String tableName;
    String pkName;
    List<DbColumnInfo> columns = new ArrayList<DbColumnInfo>();
}
