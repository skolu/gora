package org.db.gora.sqlite;

import android.util.Log;

import java.util.Comparator;

final class LongValueComparator implements Comparator<Object> {
	public LongValueComparator(ValueAccess getter) {
		this.getter = getter;
	}
	@Override
	public int compare(Object lhs, Object rhs) {
		int result = 0;
		try {
			long id1 = (long) (Long) getter.getValue(lhs);
			long id2 = (long) (Long) getter.getValue(lhs);
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
	private final ValueAccess getter;
} 

