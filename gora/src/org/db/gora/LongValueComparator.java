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

import android.util.Log;

import java.util.Comparator;

/**
  * Record ID {@link Comparator}
 *
  * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final class LongValueComparator implements Comparator<Object> {
	public LongValueComparator(ColumnAccessor getter) {
		this.getter = getter;
	}
	@Override
	public int compare(Object lhs, Object rhs) {
		int result = 0;
		try {
			long id1 = (Long) getter.getValue(lhs);
			long id2 = (Long) getter.getValue(rhs);
			if (id1 < id2) {
				result = -1;
			} 
			else if (id1 > id2) {
				result = 1;
			}
		} catch (Exception e) {
			Log.e(Settings.TAG, "LongValueComparator", e);
		}
		return result;
	}
	private final ColumnAccessor getter;
} 

