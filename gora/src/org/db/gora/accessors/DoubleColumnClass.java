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

package org.db.gora.accessors;

/**
 * Enums the supported java types that are mapped into SQLite REAL
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

enum DoubleColumnClass {
    Double,
    Float,
    BigDecimal;

    static DoubleColumnClass resolveType(Class<?> type) {
        if (type == java.lang.Double.TYPE) {
            return DoubleColumnClass.Double;
        } else if (type == java.lang.Float.TYPE) {
            return DoubleColumnClass.Float;
        } else if (type == java.math.BigDecimal.class) {
            return DoubleColumnClass.BigDecimal;
        } else {
            return DoubleColumnClass.Double;
        }
    }
}
